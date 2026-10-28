package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.port.OrderLoyaltyBridge;
import com.utetra.order.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWorkflowService {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final OrderSupport support;
  private final OrderLoyaltyBridge loyalty;
  private final OrderPromotionService promos;
  private final Clock clock;
  private final Duration returnWindow;
  private final boolean shipperEnabled;
  private final OrderInventoryService inventory;
  private final org.springframework.context.ApplicationEventPublisher rewardEvents;

  public OrderWorkflowService(
      OrderDataRepository data,
      CommerceReferenceRepository refs,
      OrderSupport support,
      OrderLoyaltyBridge loyalty,
      OrderPromotionService promos,
      Clock clock,
      @Value("${order.return-window:3h}") Duration returnWindow,
      @Value("${features.shipper.enabled:false}") boolean shipperEnabled, OrderInventoryService inventory,org.springframework.context.ApplicationEventPublisher rewardEvents) {
    this.data = data;
    this.refs = refs;
    this.support = support;
    this.loyalty = loyalty;
    this.promos = promos;
    this.clock = clock;
    this.returnWindow = returnWindow;
    this.shipperEnabled = shipperEnabled;
    this.inventory = inventory;
    this.rewardEvents=rewardEvents;
  }

  /** Match checkout/group's user -> order order before any loyalty mutation.
   * The order's buyer is immutable; locate it without locking or caching the order.
   * Lock only the row here: a blocked buyer must not prevent an operator's refund.
   */
  private Order lockForLoyalty(long id) {
    long userId = CommerceReferenceRepository.number(
        refs.one("select user_id from orders where id=?", id), "user_id");
    refs.one("select id from users where id=? for update", userId);
    return data.require(Order.class, id, true);
  }

  public void shipper(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!shipperEnabled || !actor.is("SHIPPER")) throw OrderProblem.forbidden();
  }

  private boolean assigned(Actor actor, long order) {
    return !data.query(
            DeliveryAssignment.class,
            "where e.orderId=:order and e.shipperId=:user and e.status='ACTIVE'",
            Map.of("order", order, "user", actor.userId()),
            false,
            1)
        .isEmpty();
  }

  public void readable(Actor actor, Order o) {
    refs.requireCurrent(actor, false);
    if (actor.buyer() && (o.userId == actor.userId() || o.groupOrderId!=null&&!data.query(GroupOrderMember.class,"where e.groupOrderId=:group and e.orderId=:order and e.userId=:user",Map.of("group",o.groupOrderId,"order",o.id,"user",actor.userId()),false,1).isEmpty())) return;
    if (refs.operates(actor, o.storeId)) return;
    if (shipperEnabled
        && actor.is("SHIPPER")
        && !data.query(
                DeliveryAssignment.class,
                "where e.orderId=:order and e.shipperId=:user and e.status in"
                    + " ('ACTIVE','COMPLETED')",
                Map.of("order", o.id, "user", actor.userId()),
                false,
                1)
            .isEmpty()) return;
    throw OrderProblem.missing();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> read(Actor actor, long id) {
    Order o = data.require(Order.class, id, false);
    readable(actor, o);
    Map<String, Object> result = new LinkedHashMap<>(support.view(o));
    if("LOCAL_QR".equals(o.paymentMethod)){var payments=data.query(LocalQrPayment.class,"where e.orderId=:order",Map.of("order",id),false,100);result.put("qrCollected",payments.stream().filter(p->Set.of("SUCCESS","REFUNDED").contains(p.status)).map(p->p.amount).reduce(BigDecimal.ZERO,BigDecimal::add));result.put("qrRefundDue",payments.stream().filter(p->"SUCCESS".equals(p.status)&&"REFUND_PENDING".equals(o.paymentStatus)).map(p->p.amount).reduce(BigDecimal.ZERO,BigDecimal::add));result.put("qrRefunded",payments.stream().filter(p->"REFUNDED".equals(p.status)).map(p->p.amount).reduce(BigDecimal.ZERO,BigDecimal::add));}
    var assignments = data.query(DeliveryAssignment.class, "where e.orderId=:order", Map.of("order",id), false,1);
    result.put("delivery", assignments.stream().map(a -> {
      Map<String,Object> delivery = new LinkedHashMap<>();
      delivery.put("shipperId",a.shipperId);
      delivery.put("name",refs.one("select full_name from users where id=?",a.shipperId).get("full_name"));
      delivery.put("status",a.status);
      delivery.put("assignedAt",a.assignedAt.toString());
      delivery.put("completedAt",a.completedAt==null ? "" : a.completedAt.toString());
      delivery.put("events",data.query(DeliveryEvent.class,"where e.assignmentId=:assignment order by e.createdAt,e.id",Map.of("assignment",a.id),false,200));
      return delivery;
    }).toList());
    result.put("canShip",actor.is("SHIPPER") && assigned(actor,id)
        && Set.of("CONFIRMED","PICKED_UP","DELIVERING").contains(o.status));
    if("LOCAL_QR".equals(o.paymentMethod)){var ledger=data.query(LocalQrPayment.class,"where e.orderId=:order",Map.of("order",id),false,100);result.put("paidCount",ledger.stream().filter(p->"SUCCESS".equals(p.status)).count());result.put("payerCount",ledger.size());var mine=ledger.stream().filter(p->p.userId==actor.userId()).findFirst();result.put("myShare",mine.map(p->p.amount).orElse(BigDecimal.ZERO));result.put("myPaymentStatus",mine.map(p->p.status).orElse("HOST"));}
    result.put("owner",o.userId==actor.userId());result.put("groupOrder",o.groupOrderId!=null);
    result.put("qrPayer","LOCAL_QR".equals(o.paymentMethod)&&!data.query(LocalQrPayment.class,"where e.orderId=:order and e.userId=:user",Map.of("order",o.id,"user",actor.userId()),false,1).isEmpty());
    if(o.groupOrderId!=null)result.put("groupToken",refs.one("select invite_token from group_orders where id=?",o.groupOrderId).get("invite_token"));
    result.put("received",o.receivedAt!=null);result.put("receivedAt",o.receivedAt==null?"":o.receivedAt.toString());
    if (actor.is("SHIPPER")) {
      var pickup = refs.store(o.storeId,false);
      result.put("pickupAddress",pickup.get("address"));
      result.put("pickupPhone",pickup.get("phone"));
    }
    result.put("appliedVouchers",data.query(CouponRedemption.class,"where e.orderId=:order order by e.id",Map.of("order",id),false,10).stream().map(r->Map.of("name",Objects.toString(r.promotionNameSnapshot,"Ưu đãi"),"discount",r.discountSnapshot)).toList());
    result.put("deliveryAreaName",o.deliveryAreaName);result.put("pointsUsed",o.pointsUsed);result.put("recipientName", o.recipientName);
    result.put("recipientPhone", o.recipientPhone);
    result.put("deliveryAddress", o.deliveryAddress);
    result.put("carrierName", o.carrierName);
    result.put("note", o.note == null ? "" : o.note);
    result.put(
        "items",
        data.items(o.id).stream()
            .map(
                i ->
                    Map.of(
                        "id",
                        i.id,
                        "name",
                        i.productName,
                        "size",
                        i.sizeName,
                        "quantity",
                        i.quantity,
                        "sugar",
                        i.sugarLevel,
                        "ice",
                        i.iceLevel,
                        "note",
                        i.note,
                        "total",
                        i.lineTotal,
                        "toppings",
                        data.toppings(i.id).stream()
                            .map(t -> Map.of("name", t.toppingName, "price", t.unitPrice))
                            .toList()))
            .toList());
    result.put(
        "history",
        data
            .query(
                OrderStatusHistory.class,
                "where e.orderId=:order order by e.createdAt,e.id",
                Map.of("order", o.id),
                false,
                200)
            .stream()
            .map(
                h ->
                    Map.of(
                        "status",
                        h.toStatus,
                        "type",
                        h.eventType,
                        "createdAt",
                        h.createdAt.toString(),
                        "reason",
                        h.reason == null ? "" : h.reason))
            .toList());
    result.put(
        "returns",
        data
            .query(ReturnRequest.class, "where e.orderId=:order", Map.of("order", o.id), false, 1)
            .stream()
            .map(
                r ->
                    Map.of(
                        "id",
                        r.id,
                        "reason",
                        r.reason,
                        "decisionReason",
                        r.decisionReason == null ? "" : r.decisionReason,
                        "status",
                        r.status,
                        "waivePhysicalReturn",
                        r.waivePhysicalReturn,
                        "evidenceUrl",
                        r.evidenceUrl == null ? "" : r.evidenceUrl))
            .toList());
    return result;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(
      Actor actor, String mode, String status, int page, int size) {
    refs.requireCurrent(actor, false);
    if (page < 0 || page > 499 || size < 1 || size > 50)
      throw OrderProblem.invalid("Phân trang không hợp lệ.");
    String where;
    Map<String, Object> params = new HashMap<>();
    if ("buyer".equals(mode)) {
      support.buyer(actor);
      where = "where (e.userId=:user or exists (select m.id from GroupOrderMember m where m.orderId=e.id and m.groupOrderId=e.groupOrderId and m.userId=:user))";
      params.put("user", actor.userId());
    } else if ("shipper".equals(mode)) {
      shipper(actor);
      where = "where exists (select a.id from DeliveryAssignment a where a.orderId=e.id"
          + " and a.shipperId=:user and a.status in ('ACTIVE','COMPLETED'))";
      params.put("user", actor.userId());
    } else {
      if (!actor.is("ADMIN") && !actor.is("MANAGER") && !actor.is("VENDOR"))
        throw OrderProblem.forbidden();
      var ids =
          refs.list("select id from stores").stream()
              .map(s -> CommerceReferenceRepository.number(s, "id"))
              .filter(id -> refs.operates(actor, id))
              .toList();
      if (ids.isEmpty()) return List.of();
      where = "where e.storeId in :ids";
      params.put("ids", ids);
    }
    if("RECEIVED".equals(status)){where+=" and e.receivedAt is not null";}else if (status != null && !status.isBlank()) {
      if (!Set.of(
              "NEW",
              "CONFIRMED",
              "PICKED_UP",
              "DELIVERING",
              "DELIVERED",
              "CANCELLED",
              "RETURN_REQUESTED",
              "RETURN_APPROVED",
              "RETURNED",
              "REFUNDED")
          .contains(status)) throw OrderProblem.invalid("Trạng thái không hợp lệ.");
      where += " and e.status=:status";
      params.put("status", status);
    }
    return data
        .query(
            Order.class,
            where + " order by e.createdAt desc,e.id desc",
            params,
            false,
            Math.min(10000, (page + 1) * size))
        .stream()
        .skip((long) page * size)
        .map(o -> {
          Map<String,Object> view = support.view(o);
          if ("shipper".equals(mode)) {
            var pickup = refs.store(o.storeId,false);
            view.put("pickupAddress",pickup.get("address"));
            view.put("pickupPhone",pickup.get("phone"));
            view.put("recipientName", o.recipientName);
            view.put("recipientPhone", o.recipientPhone);
            view.put("deliveryAddress", o.deliveryAddress);
            view.put("codDue", "COD".equals(o.paymentMethod) && !"PAID".equals(o.paymentStatus)
                ? o.finalAmount : BigDecimal.ZERO);
          }
          return view;
        })
        .toList();
  }

  private void version(Order o, long version) {
    if (o.version != version)
      throw OrderProblem.conflict("Đơn đã thay đổi; hãy tải lại trước khi thao tác.");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String,Object> confirmReceived(Actor actor,long id){
    support.buyer(actor);refs.requireCurrent(actor,false);var order=data.require(Order.class,id,true);
    if(!order.userId.equals(actor.userId()))throw OrderProblem.forbidden();
    if(order.receivedAt!=null)return read(actor,id);
    if(!"DELIVERED".equals(order.status)||!"PAID".equals(order.paymentStatus))throw OrderProblem.conflict("Chỉ xác nhận nhận hàng khi đơn đã giao và hoàn tất thanh toán.");
    order.receivedAt=clock.instant();order.updatedAt=clock.instant();support.event(order,actor,order.status,"CUSTOMER_RECEIVED","Khách hàng đã xác nhận nhận hàng.");data.flush();return read(actor,id);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> transition(Actor actor, long id, Transition request) {
    refs.requireCurrent(actor, false);
    Order o = lockForLoyalty(id);
    version(o, request.version());
    boolean shipping = actor.is("SHIPPER");
    if (shipping) {
      shipper(actor);
      if (!assigned(actor, id)) throw OrderProblem.forbidden();
    } else refs.requireOperator(actor, o.storeId);
    List<String> states = List.of("NEW", "CONFIRMED", "PICKED_UP", "DELIVERING", "DELIVERED");
    int position = states.indexOf(o.status);
    if (position < 0
        || position == states.size() - 1
        || !states.get(position + 1).equals(request.status())
        || shipping && position < 1)
      throw OrderProblem.conflict("Không được bỏ bước hoặc đảo trạng thái đơn.");
    LocalQrPaymentService.assertLedger(data,refs,o,true);
    if (!"COD".equals(o.paymentMethod) && !"PAID".equals(o.paymentStatus))
      throw OrderProblem.conflict("Chưa xác nhận thanh toán trực tuyến.");
    String from = o.status;
    o.status = request.status();
    o.updatedAt = clock.instant();
    if ("DELIVERED".equals(o.status)) {
      if ("COD".equals(o.paymentMethod)) {
        if (!request.cashCollected())
          throw OrderProblem.invalid("Cần xác nhận đã thu đủ tiền COD.");
        o.paymentStatus = "PAID";
        o.paidAt = clock.instant();
        o.paymentConfirmedBy = actor.userId();
        var p =
            data.query(
                    PaymentTransaction.class,
                    "where e.orderId=:order and e.provider='COD'",
                    Map.of("order", id),
                    true,
                    1)
                .getFirst();
        p.status = "SUCCEEDED";
        p.paidAt = o.paidAt;
        p.updatedAt = clock.instant();
      }
      if (!"PAID".equals(o.paymentStatus)) throw OrderProblem.conflict("Đơn chưa trả đủ tiền.");
      o.deliveredAt = clock.instant();
      BigDecimal goods = o.subtotal.subtract(o.productDiscount).subtract(o.pointsDiscount);
      o.pointsEarned = goods.divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValueExact();
      loyalty.record(o.userId, o.id, "EARN", o.pointsEarned);
      promos.status(o.id, "USED");
      StoreSettlement s = new StoreSettlement();
      s.orderId = o.id;
      s.storeId = o.storeId;
      s.goodsNet = goods;
      s.commissionRate = o.commissionRate;
      s.commissionAmount = o.commissionAmount;
      s.shippingNet = o.shippingGross.subtract(o.shippingDiscount);
      s.storeNet = o.storeNet;
      s.updatedAt = clock.instant();
      data.save(s);
      var assignments =
          data.query(
              DeliveryAssignment.class, "where e.orderId=:order", Map.of("order", id), true, 1);
      for (var a : assignments) {
        a.status = "COMPLETED";
        a.completedAt = clock.instant();
      }
    }
    if (shipping) {
      DeliveryEvent ev = new DeliveryEvent();
      ev.assignmentId =
          data.query(
                  DeliveryAssignment.class, "where e.orderId=:order", Map.of("order", id), false, 1)
              .getFirst()
              .id;
      ev.actorId = actor.userId();
      ev.type = o.status;
      ev.note = OrderSupport.note(request.reason(), 500);
      ev.createdAt = clock.instant();
      data.save(ev);
    }
    support.audit(actor, "ORDER_TRANSITION", id);
    support.event(o, actor, from, "TRANSITION", request.reason());
    if("DELIVERED".equals(o.status))rewardEvents.publishEvent(new com.utetra.identity.OfferDeliveryReward(o.id));
    return support.view(o);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> cancel(Actor actor, long id, long version, String reason) {
    refs.requireCurrent(actor, false);
    Order o = lockForLoyalty(id);
    version(o, version);
    boolean owner = actor.buyer() && o.userId == actor.userId();
    boolean operator = refs.operates(actor, o.storeId);
    if (!owner && !operator) throw OrderProblem.forbidden();
    if("VNPAY".equals(o.paymentMethod) && "REVIEW".equals(o.paymentStatus))
      throw OrderProblem.conflict("Giao dịch đang chờ đối soát cổng; chưa thể hủy hoặc thanh toán lại.");
    if (owner && !operator && !"NEW".equals(o.status)
        || !Set.of("NEW", "CONFIRMED", "PICKED_UP", "DELIVERING").contains(o.status))
      throw OrderProblem.conflict("Đơn không còn trong thời điểm cho phép hủy.");
    BigDecimal localCollected=BigDecimal.ZERO;
    if("LOCAL_QR".equals(o.paymentMethod)){LocalQrPaymentService.assertLedger(data,refs,o,false);localCollected=data.query(LocalQrPayment.class,"where e.orderId=:order and e.status='SUCCESS'",Map.of("order",id),false,100).stream().map(p->p.amount).reduce(BigDecimal.ZERO,BigDecimal::add);}
    boolean localRefundPending=localCollected.signum()>0;
    boolean prepaidBank = localRefundPending || Set.of("BANK_TRANSFER","VNPAY","LOCAL_QR").contains(o.paymentMethod) && "PAID".equals(o.paymentStatus);
    if ("PAID".equals(o.paymentStatus) && !prepaidBank)
      throw OrderProblem.conflict(
          "Đơn trả trước cần quy trình hoàn ngân hàng; tích hợp này đang được hoãn.");
    String from = o.status;
    o.status = "CANCELLED";
    o.paymentStatus = prepaidBank ? "REFUND_PENDING" : "CANCELLED";
    o.cancelReason = OrderSupport.required(reason, 500);
    o.cancelledAt = clock.instant();
    o.updatedAt = clock.instant();
    loyalty.record(o.userId, id, "REFUND", o.pointsUsed);
    inventory.releaseBeforePreparation(o,from);
    promos.status(id, "RELEASED");
    for (var assignment :
        data.query(
            DeliveryAssignment.class,
            "where e.orderId=:order and e.status='ACTIVE'",
            Map.of("order", id),
            true,
            1)) {
      assignment.status = "CANCELLED";
    }
    for (var p :
        data.query(
            PaymentTransaction.class, "where e.orderId=:order", Map.of("order", id), true, 20)) {
      if (!"SUCCEEDED".equals(p.status)&&!(localRefundPending&&"LOCAL_QR".equals(p.provider))) p.status = "FAILED";
      p.updatedAt = clock.instant();
    }
    support.audit(actor, "ORDER_CANCEL", id);
    support.event(o, actor, from, "CANCELLED", reason);
    return support.view(o);
  }

  @Transactional(readOnly = true)
  public void validateReturnEligibility(Actor actor, long id) {
    support.buyer(actor);
    Order o = data.require(Order.class, id, false);
    if (o.userId != actor.userId()) throw OrderProblem.missing();
    if (!"DELIVERED".equals(o.status)
        || !"PAID".equals(o.paymentStatus)
        || o.deliveredAt == null
        || !clock.instant().isBefore(o.deliveredAt.plus(returnWindow)))
      throw OrderProblem.conflict(
          "Chỉ yêu cầu trả hàng trong 3 tiếng sau giao thành công và thanh toán.");
    requireNoPreviousReturn(id);
  }

  private void requireNoPreviousReturn(long id) {
    if (!data.query(ReturnRequest.class, "where e.orderId=:order", Map.of("order", id), false, 1)
        .isEmpty())
      throw OrderProblem.conflict(
          "Mỗi đơn chỉ có một yêu cầu trả hàng; xem kết quả và lý do quyết định đã ghi.");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> requestReturn(Actor actor, long id, String reason) {
    return requestReturnWithEvidence(actor, id, reason, null);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> requestReturnWithEvidence(
      Actor actor, long id, String reason, com.utetra.dto.MediaUploadResult evidence) {
    support.buyer(actor);
    Order o = data.require(Order.class, id, true);
    if (o.userId != actor.userId()) throw OrderProblem.missing();
    if (!"DELIVERED".equals(o.status)
        || !"PAID".equals(o.paymentStatus)
        || o.deliveredAt == null
        || !clock.instant().isBefore(o.deliveredAt.plus(returnWindow)))
      throw OrderProblem.conflict(
          "Chỉ yêu cầu trả hàng trong 3 tiếng sau giao thành công và thanh toán.");
    requireNoPreviousReturn(id);
    ReturnRequest r = new ReturnRequest();
    r.orderId = id;
    r.requestedBy = actor.userId();
    r.reason = OrderSupport.required(reason, 2000);
    if (evidence != null) {
      r.evidenceUrl = evidence.secureUrl();
      r.evidencePublicId = evidence.publicId();
    }
    r.status = "REQUESTED";
    r.waivePhysicalReturn = false;
    r.createdAt = clock.instant();
    r.updatedAt = clock.instant();
    data.save(r);
    String from = o.status;
    o.status = "RETURN_REQUESTED";
    o.updatedAt = clock.instant();
    support.event(o, actor, from, "RETURN_REQUESTED", "");
    return support.view(o);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> reviewReturn(Actor actor, long id, Decision request) {
    Order o = data.require(Order.class, id, true);
    refs.requireOperator(actor, o.storeId);
    version(o, request.version());
    if (!"RETURN_REQUESTED".equals(o.status))
      throw OrderProblem.conflict("Không có yêu cầu đang chờ xử lý.");
    ReturnRequest r =
        data.query(ReturnRequest.class, "where e.orderId=:order", Map.of("order", id), true, 1)
            .getFirst();
    r.reviewedBy = actor.userId();
    r.decisionReason = OrderSupport.required(request.reason(), 1000);
    r.waivePhysicalReturn = request.waivePhysicalReturn();
    r.status = request.approve() ? "APPROVED" : "REJECTED";
    r.updatedAt = clock.instant();
    String from = o.status;
    o.status = request.approve() ? "RETURN_APPROVED" : "DELIVERED";
    o.updatedAt = clock.instant();
    support.audit(actor, "RETURN_REVIEW", id);
    support.event(
        o,
        actor,
        from,
        "RETURN_REVIEW",
        request.reason().substring(0, Math.min(500, request.reason().length())));
    return support.view(o);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> receiveReturn(Actor actor, long id, long version) {
    Order o = data.require(Order.class, id, true);
    refs.requireOperator(actor, o.storeId);
    version(o, version);
    if (!"RETURN_APPROVED".equals(o.status))
      throw OrderProblem.conflict("Yêu cầu chưa được duyệt.");
    ReturnRequest r =
        data.query(ReturnRequest.class, "where e.orderId=:order", Map.of("order", id), true, 1)
            .getFirst();
    r.status = "RECEIVED";
    r.updatedAt = clock.instant();
    String from = o.status;
    o.status = "RETURNED";
    o.updatedAt = clock.instant();
    support.event(o, actor, from, "RETURNED", "");
    return support.view(o);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> refundCash(Actor actor, long id, RefundInput request) {
    Order o = lockForLoyalty(id);
    refs.requireOperator(actor, o.storeId);
    boolean bank = Set.of("BANK_TRANSFER","VNPAY","LOCAL_QR").contains(o.paymentMethod);
    if (bank && !actor.is("ADMIN")) throw OrderProblem.forbidden();
    version(o, request.version());
    boolean cancelledBank = bank && "CANCELLED".equals(o.status) && "REFUND_PENDING".equals(o.paymentStatus);
    ReturnRequest r =
        data
            .query(ReturnRequest.class, "where e.orderId=:order", Map.of("order", id), true, 1)
            .stream()
            .findFirst()
            .orElse(null);
    if (!cancelledBank && ((!"COD".equals(o.paymentMethod) && !bank)
        || !"PAID".equals(o.paymentStatus) || r == null
        || !("RETURNED".equals(o.status)
            || "RETURN_APPROVED".equals(o.status) && r.waivePhysicalReturn)))
      throw OrderProblem.conflict("Chưa đủ điều kiện hoàn tiền cả đơn.");
    boolean cancelledLocalQr=cancelledBank&&"LOCAL_QR".equals(o.paymentMethod);
    BigDecimal refundAmount=o.finalAmount;
    if(cancelledLocalQr){LocalQrPaymentService.assertLedger(data,refs,o,false);refundAmount=data.query(LocalQrPayment.class,"where e.orderId=:order and e.status='SUCCESS'",Map.of("order",id),false,100).stream().map(payment->payment.amount).reduce(BigDecimal.ZERO,BigDecimal::add);if(refundAmount.signum()<=0||refundAmount.compareTo(o.finalAmount)>0)throw OrderProblem.conflict("Không có phần thanh toán hợp lệ để hoàn.");}
    PaymentTransaction p =
        data.query(
                PaymentTransaction.class,
                cancelledLocalQr ? "where e.orderId=:order and e.provider='LOCAL_QR' and e.status in ('PENDING','SUCCEEDED')" : "where e.orderId=:order and e.status='SUCCEEDED'",
                Map.of("order", id),
                true,
                1)
            .getFirst();
    if (p.amount.compareTo(o.finalAmount) != 0)
      throw OrderProblem.conflict(
          "Khoản thu không khớp tổng đơn; cần đối soát trước khi hoàn tiền.");
    if (!data.query(
            RefundTransaction.class,
            "where e.orderId=:order and e.status='SUCCEEDED'",
            Map.of("order", id),
            false,
            1)
        .isEmpty()) throw OrderProblem.conflict("Đơn đã được hoàn tiền.");
    RefundTransaction refund = new RefundTransaction();
    refund.orderId = id;
    refund.paymentId = p.id;
    refund.returnRequestId = r == null ? null : r.id;
    refund.refundRef = "RF-" + UUID.randomUUID();
    refund.amount = refundAmount;
    refund.status = "SUCCEEDED";
    refund.reason = cancelledBank ? o.cancelReason : r.reason;
    refund.evidenceRef = OrderSupport.required(request.evidence(), 500);
    refund.approvedBy = actor.userId();
    refund.createdAt = clock.instant();
    refund.completedAt = clock.instant();
    data.save(refund);
    o.paymentStatus = "REFUNDED";
    if (!cancelledBank) {
      loyalty.record(o.userId, id, "REFUND", o.pointsUsed);
      loyalty.record(o.userId, id, "REVERSAL", o.pointsEarned);
      promos.status(id, "RELEASED");
      StoreSettlement s =
        data.query(StoreSettlement.class, "where e.orderId=:order", Map.of("order", id), true, 1)
            .getFirst();
    s.refundedGoods = s.goodsNet;
    s.refundedShipping = s.shippingNet;
    s.commissionReversed = s.commissionAmount;
    s.updatedAt = clock.instant();
      r.status = "REFUNDED";
      r.updatedAt = clock.instant();
    }
    String from = o.status;
    if("LOCAL_QR".equals(o.paymentMethod)){for(var payment:data.query(LocalQrPayment.class,"where e.orderId=:order and e.status='SUCCESS'",Map.of("order",id),true,100))payment.status="REFUNDED";p.status="REFUNDED";p.updatedAt=clock.instant();}
    if(!cancelledLocalQr)o.status = "REFUNDED";
    o.paymentStatus = "REFUNDED";
    o.updatedAt = clock.instant();
    support.audit(actor, bank ? "BANK_REFUND" : "CASH_REFUND", id);
    support.event(o, actor, from, "REFUNDED", "");
    return support.view(o);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> deliveryFailure(Actor actor, long id, long version, String reason) {
    shipper(actor);
    Order o = data.require(Order.class, id, true);
    version(o, version);
    if (!assigned(actor, id)) throw OrderProblem.forbidden();
    if (!Set.of("PICKED_UP", "DELIVERING").contains(o.status))
      throw OrderProblem.conflict("Chỉ báo giao thất bại trong quá trình giao.");
    String detail = OrderSupport.required(reason, 500);
    DeliveryAssignment a =
        data.query(DeliveryAssignment.class, "where e.orderId=:order", Map.of("order", id), true, 1)
            .getFirst();
    DeliveryEvent ev = new DeliveryEvent();
    ev.assignmentId = a.id;
    ev.actorId = actor.userId();
    ev.type = "FAILED";
    ev.note = detail;
    ev.createdAt = clock.instant();
    data.save(ev);
    o.updatedAt =
        clock.instant().isAfter(o.updatedAt) ? clock.instant() : o.updatedAt.plusNanos(1000);
    support.event(o, actor, o.status, "DELIVERY_FAILED", detail);
    return support.view(o);
  }

  @Transactional(readOnly = true)
  public List<Map<String,Object>> availableShippers(Actor actor, long orderId) {
    if (!shipperEnabled) return List.of();
    Order order = data.require(Order.class,orderId,false);
    refs.requireOperator(actor,order.storeId);
    return refs.list("select u.id,u.full_name from users u join roles r on r.id=u.role_id"
        + " where r.name='ROLE_SHIPPER' and u.is_active=true and u.email_verified_at is not null"
        + " order by u.full_name,u.id");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void assign(Actor actor, long id, Assignment request) {
    if (!shipperEnabled) throw OrderProblem.forbidden();
    Order o = data.require(Order.class, id, true);
    refs.requireOperator(actor, o.storeId);
    version(o, request.version());
    if (!Set.of("NEW", "CONFIRMED", "PICKED_UP", "DELIVERING").contains(o.status))
      throw OrderProblem.conflict("Đơn không thể phân công.");
    if (!"ROLE_SHIPPER".equals(refs.user(request.shipperId(), true).get("role_name")))
      throw OrderProblem.invalid("Người nhận không phải Shipper hoạt động.");
    DeliveryAssignment a =
        data
            .query(DeliveryAssignment.class, "where e.orderId=:order", Map.of("order", id), true, 1)
            .stream()
            .findFirst()
            .orElse(null);
    boolean fresh = a == null;
    if (!fresh && a.shipperId.equals(request.shipperId()) && "ACTIVE".equals(a.status)) return;
    Long previous = fresh ? null : a.shipperId;
    if (fresh) {
      a = new DeliveryAssignment();
      a.orderId = id;
    }
    a.shipperId = request.shipperId();
    a.assignedBy = actor.userId();
    a.status = "ACTIVE";
    a.assignedAt = clock.instant();
    a.completedAt = null;
    if (fresh) data.save(a);
    data.flush();
    DeliveryEvent assignedEvent = new DeliveryEvent();
    assignedEvent.assignmentId = a.id;
    assignedEvent.actorId = actor.userId();
    assignedEvent.type = fresh ? "ASSIGNED" : "REASSIGNED";
    assignedEvent.note = "shipper:" + (previous == null ? "none" : previous) + "->" + a.shipperId;
    assignedEvent.createdAt = clock.instant();
    data.save(assignedEvent);
    String from = o.status;
    o.updatedAt =
        clock.instant().isAfter(o.updatedAt) ? clock.instant() : o.updatedAt.plusNanos(1000);
    support.audit(actor, "SHIPPER_ASSIGN", id);
    support.event(o, actor, from, "ASSIGNED", "");
  }
}
