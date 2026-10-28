package com.utetra.order.service;

import static com.utetra.order.repository.CommerceReferenceRepository.*;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.port.OrderLoyaltyBridge;
import com.utetra.order.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final OrderSupport support;
  private final OrderPromotionService promos;
  private final OrderLoyaltyBridge loyalty;
  private final Clock clock;
  private final BankTransferSettings bank;
  private final OrderInventoryService inventory;
  private final VnpaySettings vnpay;
  private final GroupOrderService groups;
  private final LocalQrPaymentService localQr;

  public CheckoutService(
      OrderDataRepository data,
      CommerceReferenceRepository refs,
      OrderSupport support,
      OrderPromotionService promos,
      OrderLoyaltyBridge loyalty,
      Clock clock, BankTransferSettings bank, OrderInventoryService inventory,VnpaySettings vnpay,GroupOrderService groups,LocalQrPaymentService localQr) {
    this.data = data;
    this.refs = refs;
    this.support = support;
    this.promos = promos;
    this.loyalty = loyalty;
    this.clock = clock;
    this.bank = bank;
    this.inventory = inventory;
    this.vnpay = vnpay;this.groups=groups;this.localQr=localQr;
  }
  private String method(Checkout request) {
    if("LOCAL_QR".equals(request.paymentMethod())){if(!localQr.available())throw OrderProblem.conflict("Ví QR local chưa bật.");return "LOCAL_QR";}
    if("VNPAY".equals(request.paymentMethod())) {
      if(!vnpay.available()) throw OrderProblem.conflict("VNPAY chưa được cấu hình merchant.");
      return "VNPAY";
    }
    return bank.method(request.paymentMethod());
  }

  record Line(
      CartItem item,
      Map<String, Object> variant,
      List<Map<String, Object>> toppings,
      BigDecimal toppingTotal,
      BigDecimal total) {}

  record Quote(
      Cart cart,
      Map<String, Object> store,
      Map<String, Object> address,
      Map<String, Object> area,
      Carrier carrier,
      CommissionPolicy commission,
      List<Line> lines,
      BigDecimal subtotal,
      GroupOrderService.Benefit group,
      BigDecimal productDiscount,
      BigDecimal shipping,
      BigDecimal shippingDiscount,
      BigDecimal pointsDiscount,
      BigDecimal goodsNet,
      BigDecimal commissionAmount,
      BigDecimal total,
      String hash) {}

  private Quote calculate(Actor actor, Checkout request, boolean lock) {
    support.buyer(actor);
    String paymentMethod = method(request);
    if (request.points() < 0) throw OrderProblem.invalid("Điểm không hợp lệ.");
    Cart cart = data.cart(actor.userId(), lock);
    if (cart == null || data.selectedItems(cart.id).isEmpty())
      throw OrderProblem.invalid("Giỏ hàng trống.");
    var store = refs.store(cart.storeId, lock);
    var address = refs.address(actor.userId(), request.addressId());
    var area = refs.area(cart.storeId, request.areaId());
    Carrier carrier = data.require(Carrier.class, request.carrierId(), lock);
    if(carrier.managedStoreId!=null && !carrier.managedStoreId.equals(cart.storeId))
      throw OrderProblem.invalid("Nhà vận chuyển không phục vụ cửa hàng này.");
    if (!carrier.isActive) throw OrderProblem.conflict("Nhà vận chuyển đã ngừng hoạt động.");
    var rates =
        data.query(
            CarrierAreaRate.class,
            "where e.carrierId=:carrier and e.deliveryAreaId=:area and e.isActive=true",
            Map.of("carrier", carrier.id, "area", request.areaId()),
            lock,
            1);
    if (rates.isEmpty()) throw OrderProblem.invalid("Nhà vận chuyển không phục vụ khu vực này.");
    CommissionPolicy commission =
        data.require(CommissionPolicy.class, number(store, "commission_policy_id"), lock);
    if (!commission.isActive
        || commission.ratePercent.signum() < 0
        || commission.ratePercent.compareTo(new BigDecimal("100")) > 0)
      throw OrderProblem.conflict("Chính sách hoa hồng không hợp lệ.");
    List<Line> lines = new ArrayList<>();
    Map<Long, BigDecimal> products = new TreeMap<>();
    BigDecimal subtotal = BigDecimal.ZERO;
    int units = 0;
    var selectedItems = data.selectedItems(cart.id);
    Map<Long, Map<String,Object>> lockedVariants = new HashMap<>();
    // Each call executes a bound, single-variant FOR UPDATE before any per-line topping locks.
    // Keep cart/snapshot order separate from the order in which locks are acquired.
    if (lock) for (long variantId : selectedItems.stream().map(i -> i.variantId).distinct().sorted().toList())
      lockedVariants.put(variantId, refs.saleVariant(variantId, true, clock));
    for (var item : selectedItems) {
      var v = lock ? lockedVariants.get(item.variantId) : refs.saleVariant(item.variantId, false, clock);
      if (number(v, "store_id") != cart.storeId)
        throw OrderProblem.invalid("Món không thuộc cửa hàng của giỏ.");
      List<Map<String, Object>> toppings = new ArrayList<>();
      BigDecimal topTotal = BigDecimal.ZERO;
      for (var ct : data.cartToppings(item.id)) {
        var top = refs.topping(ct.toppingId, number(v, "product_id"), cart.storeId, lock);
        toppings.add(top);
        topTotal = topTotal.add(OrderSupport.amount(money(top, "price")));
      }
      if (item.quantity < 1
          || item.quantity > 20
          || !Set.of(0, 30, 50, 70, 100).contains(item.sugarLevel)
          || !Set.of(0, 30, 50, 70, 100).contains(item.iceLevel))
        throw OrderProblem.invalid("Giỏ có tùy chọn không hợp lệ.");
      BigDecimal total =
          OrderSupport.amount(money(v, "base_price"))
              .add(OrderSupport.amount(money(v, "extra_price")))
              .add(topTotal)
              .multiply(BigDecimal.valueOf(item.quantity));
      subtotal = OrderSupport.amount(subtotal.add(total));
      units += item.quantity;
      products.merge(number(v, "product_id"), total, BigDecimal::add);
      lines.add(new Line(item, v, toppings, topTotal, total));
    }
    if (units > 50) throw OrderProblem.invalid("Mỗi đơn tối đa 50 ly.");
    Map<Long,Integer> quantities=new TreeMap<>();
    for(Line line:lines) quantities.merge(line.item.variantId,line.item.quantity,Math::addExact);
    inventory.check(quantities);
    // Lock program IDs in a stable order before counting quota.
    if (lock) {
      TreeSet<Long> ids = new TreeSet<>();
      if (request.productPromotionId() != null) ids.add(request.productPromotionId());
      if (request.shippingPromotionId() != null) ids.add(request.shippingPromotionId());
      for (long id : ids) refs.one("select id from promotions where id=? for update", id);
    }
    BigDecimal shipping =
        OrderSupport.amount(carrier.baseFee)
            .add(OrderSupport.amount(rates.getFirst().additionalFee));
    BigDecimal productDiscount =
        promos.discount(
            request.productPromotionId(),
            actor.userId(),
            cart.storeId,
            "PRODUCT_PERCENT",
            subtotal,
            subtotal,
            products,
            lock);
    var group=groups.benefit(actor,cart,subtotal,lock);
    if(group.id()!=null&&"COD".equals(paymentMethod))group=new GroupOrderService.Benefit(group.id(),group.participants(),group.amount(),"HOST");
    if("INDIVIDUAL".equals(group.paymentMode())&&(!Set.of("COD","BANK_TRANSFER","LOCAL_QR").contains(paymentMethod)||request.points()!=0))throw OrderProblem.invalid("Chia phần thanh toán dùng COD hoặc QR, không dùng điểm riêng của chủ nhóm.");
    if(group.amount().signum()>0) {
      if(request.productPromotionId()!=null)throw OrderProblem.invalid("Ưu đãi nhóm không cộng dồn mã giảm giá sản phẩm. Hãy bỏ mã sản phẩm để tiếp tục.");
      productDiscount=group.amount();
    }
    BigDecimal shippingDiscount =
        promos.discount(
            request.shippingPromotionId(),
            actor.userId(),
            cart.storeId,
            "SHIPPING_PERCENT",
            subtotal,
            shipping,
            products,
            lock);
    BigDecimal points = BigDecimal.valueOf(request.points()).multiply(new BigDecimal("1000"));
    int max =
        subtotal
            .subtract(productDiscount)
            .multiply(new BigDecimal("0.20"))
            .divide(new BigDecimal("1000"), 0, RoundingMode.DOWN)
            .intValueExact();
    if (request.points() > max)
      throw OrderProblem.invalid("Điểm sử dụng vượt 20% giá trị hàng sau giảm.");
    BigDecimal goods = subtotal.subtract(productDiscount).subtract(points);
    BigDecimal fee =
        goods.multiply(commission.ratePercent).divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
    BigDecimal total = OrderSupport.amount(goods.add(shipping).subtract(shippingDiscount));
    if (!"COD".equals(paymentMethod) && total.signum() <= 0)
      throw OrderProblem.invalid("Đơn chuyển khoản phải có tổng tiền lớn hơn 0.");
    if("VNPAY".equals(paymentMethod) && total.multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString().length()>12)
      throw OrderProblem.invalid("Tổng tiền vượt giới hạn gửi cổng thanh toán.");
    var payload = new LinkedHashMap<String, Object>();
    payload.put("paymentMethod", paymentMethod);
    payload.put("store", store);
    payload.put("group",List.of(group.id()==null?0:group.id(),group.participants(),group.amount(),group.paymentMode()));
    payload.put("address", address);
    payload.put("area", area);
    payload.put("carrier", List.of(carrier.id, carrier.name, carrier.version));
    payload.put(
        "selection",
        lines.stream()
            .map(l -> List.of(l.item.configurationHash, l.item.quantity, l.variant, l.toppings))
            .toList());
    payload.put(
        "prices",
        List.of(
            subtotal,
            productDiscount,
            points,
            shipping,
            shippingDiscount,
            commission.ratePercent,
            total));
    return new Quote(
        cart,
        store,
        address,
        area,
        carrier,
        commission,
        lines,
        subtotal,
        group,
        productDiscount,
        shipping,
        shippingDiscount,
        points,
        goods,
        fee,
        total,
        support.hash(payload));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> quote(Actor actor, Checkout request) {
    Quote q = calculate(actor, request, false);
    return Map.of(
        "group",Map.of("participants",q.group.participants(),"discount",q.group.amount(),"paymentMode",q.group.paymentMode()),
        "subtotal",
        q.subtotal,
        "productDiscount",
        q.productDiscount,
        "pointsDiscount",
        q.pointsDiscount,
        "shipping",
        q.shipping,
        "shippingDiscount",
        q.shippingDiscount,
        "total",
        q.total,
        "hash",
        q.hash,
        "storeName",
        q.store.get("name"));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> checkout(Actor actor, Checkout request) {
    support.buyer(actor);
    String key = OrderSupport.required(request.key(), 36);
    try {
      UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw OrderProblem.invalid("Checkout key phải là UUID.");
    }
    String requestHash = support.hash(request);
    refs.user(actor.userId(), true);
    Order existing = data.checkout(actor.userId(), key);
    if (existing != null) {
      if (!existing.requestHash.equals(requestHash))
        throw OrderProblem.conflict("Checkout key đã dùng với nội dung khác.");
      return support.view(existing);
    }
    Quote q = calculate(actor, request, true);
    if (!q.hash.equals(request.quoteHash()))
      throw OrderProblem.conflict("Giá hoặc cấu hình đã thay đổi; hãy xác nhận báo giá mới.");
    Map<Long,Integer> quantities=new TreeMap<>();
    for(Line line:q.lines) quantities.merge(line.item.variantId,line.item.quantity,Math::addExact);
    Set<Long> trackedVariants=inventory.reserve(q.cart.storeId,quantities);
    Order o = new Order();
    o.orderCode = "UT-" + UUID.randomUUID().toString().replace("-", "");
    o.userId = actor.userId();
    o.storeId = q.cart.storeId;
    o.deliveryAreaId = request.areaId();
    o.carrierId = q.carrier.id;
    o.commissionPolicyId = q.commission.id;
    o.checkoutKey = key;
    o.requestHash = requestHash;
    o.recipientName = q.address.get("recipient_name").toString();
    o.recipientPhone = q.address.get("phone").toString();
    o.deliveryAddress = q.address.get("address_detail").toString();
    o.storeName = q.store.get("name").toString();
    o.deliveryAreaName = q.area.get("name").toString();
    o.carrierName = q.carrier.name;
    o.subtotal = q.subtotal;
    o.productDiscount = q.productDiscount;
    o.groupOrderId=q.group.id();o.groupParticipants=q.group.participants();o.groupDiscount=q.group.amount();
    o.pointsDiscount = q.pointsDiscount;
    o.shippingGross = q.shipping;
    o.shippingDiscount = q.shippingDiscount;
    o.finalAmount = q.total;
    o.commissionRate = q.commission.ratePercent;
    o.commissionAmount = q.commissionAmount;
    o.storeNet = q.goodsNet.subtract(q.commissionAmount);
    o.pointsUsed = request.points();
    o.pointsEarned = 0;
    o.paymentMethod = method(request);
    o.paymentStatus = "UNPAID";
    o.status = "NEW";
    o.note = OrderSupport.note(request.note(), 500);
    o.createdAt = clock.instant();
    o.updatedAt = clock.instant();
    data.save(o);
    data.flush();
    BigDecimal remaining = q.productDiscount.add(q.pointsDiscount);
    BigDecimal totalDiscount = remaining;
    for (int index = 0; index < q.lines.size(); index++) {
      Line l = q.lines.get(index);
      OrderItem i = new OrderItem();
      i.orderId = o.id;
      i.variantId = l.item.variantId;
      i.stockReserved = trackedVariants.contains(l.item.variantId);
      i.productName = l.variant.get("product_name").toString();
      i.sizeName = l.variant.get("size_name").toString();
      i.basePrice = money(l.variant, "base_price");
      i.extraPrice = money(l.variant, "extra_price");
      i.drinkUnitPrice = i.basePrice.add(i.extraPrice);
      i.toppingUnitTotal = l.toppingTotal;
      i.quantity = l.item.quantity;
      i.sugarLevel = l.item.sugarLevel;
      i.iceLevel = l.item.iceLevel;
      i.note = l.item.note;
      i.lineTotal = l.total;
      i.discountAllocated =
          index == q.lines.size() - 1
              ? remaining
              : totalDiscount.signum() == 0
                  ? BigDecimal.ZERO
                  : totalDiscount.multiply(l.total).divide(q.subtotal, 0, RoundingMode.DOWN);
      remaining = remaining.subtract(i.discountAllocated);
      data.save(i);
      data.flush();
      for (var top : l.toppings) {
        OrderItemTopping t = new OrderItemTopping();
        t.orderItemId = i.id;
        t.toppingId = number(top, "id");
        t.toppingName = top.get("name").toString();
        t.unitPrice = money(top, "price");
        data.save(t);
      }
    }
    loyalty.record(actor.userId(), o.id, "REDEEM", o.pointsUsed);
    promos.reserve(request.productPromotionId(), o.id, o.userId, q.productDiscount);
    promos.reserve(request.shippingPromotionId(), o.id, o.userId, q.shippingDiscount);
    PaymentTransaction p = new PaymentTransaction();
    p.orderId = o.id;
    p.provider = o.paymentMethod;
    p.txnRef = "VNPAY".equals(o.paymentMethod)?"VN"+UUID.randomUUID().toString().replace("-",""):o.paymentMethod + "-" + o.orderCode;
    p.amount = o.finalAmount;
    p.status = "PENDING";
    p.createdAt = clock.instant();
    p.updatedAt = clock.instant();
    if("VNPAY".equals(o.paymentMethod)) p.expiresAt=clock.instant().plus(vnpay.timeout());
    data.save(p);
    groups.ordered(q.cart,o.id);
    if("LOCAL_QR".equals(o.paymentMethod))localQr.create(o);
    for(var line:q.lines){for(var top:data.cartToppings(line.item.id))data.remove(top);data.remove(line.item);}
    q.cart.updatedAt=clock.instant();q.cart.activityRevision++;
    support.event(o, actor, null, "CREATED", "");
    return support.view(o);
  }
}
