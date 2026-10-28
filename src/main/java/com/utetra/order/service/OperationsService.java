package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationsService {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final OrderSupport support;
  private final OrderWorkflowService workflow;
  private final Clock clock;

  public OperationsService(
      OrderDataRepository data,
      CommerceReferenceRepository refs,
      OrderSupport support,
      OrderWorkflowService workflow,
      Clock clock) {
    this.data = data;
    this.refs = refs;
    this.support = support;
    this.workflow = workflow;
    this.clock = clock;
  }

  private void admin(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN")) throw OrderProblem.forbidden();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> carriers(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN") && !actor.is("MANAGER") && !actor.buyer())
      throw OrderProblem.forbidden();
    return data.query(Carrier.class, "order by e.id", Map.of(), false, 100).stream()
        .filter(c -> !actor.is("MANAGER") || c.managedStoreId==null || refs.operates(actor,c.managedStoreId))
        .map(c -> carrierView(c,actor))
        .toList();
  }

  private Map<String, Object> carrierView(Carrier c, Actor actor) {
    var result = new LinkedHashMap<String,Object>(Map.of(
        "id",
        c.id,
        "name",
        c.name,
        "baseFee",
        c.baseFee,
        "isActive",
        c.isActive,
        "version",
        c.version));
    result.put("storeId",c.managedStoreId);
    result.put("canManage",actor.is("ADMIN") || actor.is("MANAGER") && c.managedStoreId!=null && refs.operates(actor,c.managedStoreId));
    return result;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Carrier carrier(Actor actor, CarrierInput request) {
    refs.requireCurrent(actor,false);
    if(!actor.is("ADMIN") && !actor.is("MANAGER")) throw OrderProblem.forbidden();
    if(actor.is("MANAGER") && request.storeId()==null) throw OrderProblem.forbidden();
    if(request.storeId()!=null) refs.requireOperator(actor,request.storeId());
    Carrier c =
        request.id() == null ? new Carrier() : data.require(Carrier.class, request.id(), true);
    if(request.id()!=null && !Objects.equals(c.managedStoreId,request.storeId()))
      throw OrderProblem.forbidden();
    c.managedStoreId=request.storeId();
    if (request.id() != null && c.version != request.version())
      throw OrderProblem.conflict("Nhà vận chuyển đã thay đổi.");
    c.name = OrderSupport.required(request.name(), 150);
    c.baseFee = OrderSupport.amount(request.baseFee());
    c.isActive = request.active();
    c.updatedAt = clock.instant();
    if (c.id == null) {
      c.createdAt = clock.instant();
      data.save(c);
    }
    data.flush();
    support.audit(actor, "CARRIER_UPDATE", c.id);
    return c;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public CarrierAreaRate rate(Actor actor, RateInput request) {
    refs.requireCurrent(actor, false);
    var area =
        refs.one("select * from store_delivery_areas where id=? for update", request.areaId());
    long store = CommerceReferenceRepository.number(area, "store_id");
    refs.requireOperator(actor, store);
    if (!actor.is("ADMIN") && !actor.is("MANAGER")) throw OrderProblem.forbidden();
    Carrier carrier=data.require(Carrier.class, request.carrierId(), true);
    if(carrier.managedStoreId!=null && carrier.managedStoreId!=store)
      throw OrderProblem.forbidden();
    CarrierAreaRate r =
        data
            .query(
                CarrierAreaRate.class,
                "where e.carrierId=:carrier and e.deliveryAreaId=:area",
                Map.of("carrier", request.carrierId(), "area", request.areaId()),
                true,
                1)
            .stream()
            .findFirst()
            .orElse(null);
    boolean fresh = r == null;
    if (fresh) {
      r = new CarrierAreaRate();
      r.carrierId = request.carrierId();
      r.deliveryAreaId = request.areaId();
      r.createdAt = clock.instant();
    }
    r.additionalFee = OrderSupport.amount(request.fee());
    r.isActive = request.active();
    r.updatedAt = clock.instant();
    if (fresh) data.save(r);
    data.flush();
    support.audit(actor, "CARRIER_RATE", r.id);
    return r;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public CommissionPolicy commission(Actor actor, CommissionInput request) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN") && !actor.is("MANAGER")) throw OrderProblem.forbidden();
    refs.requireOperator(actor, request.storeId());
    refs.store(request.storeId(), true);
    BigDecimal percent = request.percent();
    if (percent == null
        || percent.signum() < 0
        || percent.compareTo(new BigDecimal("100")) > 0
        || percent.scale() > 2)
      throw OrderProblem.invalid("Hoa hồng từ 0 đến 100%, tối đa 2 chữ số thập phân.");
    CommissionPolicy c = new CommissionPolicy();
    c.name = "Shop " + request.storeId() + " - " + clock.instant();
    c.ratePercent = percent;
    c.isActive = true;
    c.createdAt = clock.instant();
    c.updatedAt = clock.instant();
    data.save(c);
    data.flush();
    refs.write(
        "update stores set commission_policy_id=?,updated_at=?,version=version+1 where id=?",
        c.id,
        clock.instant(),
        request.storeId());
    support.audit(actor, "COMMISSION_UPDATE", request.storeId());
    return c;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public int globalCommission(Actor actor, BigDecimal percent) {
    admin(actor);
    if (percent == null
        || percent.signum() < 0
        || percent.compareTo(new BigDecimal("100")) > 0
        || percent.scale() > 2) throw OrderProblem.invalid("Tỷ lệ hoa hồng không hợp lệ.");
    CommissionPolicy c = new CommissionPolicy();
    c.name = "Global - " + clock.instant();
    c.ratePercent = percent;
    c.isActive = true;
    c.createdAt = clock.instant();
    c.updatedAt = clock.instant();
    data.save(c);
    data.flush();
    int count = 0;
    for (var row : refs.list("select id from stores order by id for update")) {
      long id = CommerceReferenceRepository.number(row, "id");
      refs.write(
          "update stores set commission_policy_id=?,updated_at=?,version=version+1 where id=?",
          c.id,
          clock.instant(),
          id);
      support.audit(actor, "GLOBAL_COMMISSION", id);
      count++;
    }
    return count;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> stores(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN") && !actor.is("MANAGER") && !actor.is("VENDOR"))
      throw OrderProblem.forbidden();
    return refs
        .list(
            "select s.id,s.name,s.commission_policy_id,c.rate_percent commission_rate from stores s"
                + " left join commission_policies c on c.id=s.commission_policy_id order by s.id")
        .stream()
        .filter(s -> refs.operates(actor, CommerceReferenceRepository.number(s, "id")))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> deliveryAreas(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN") && !actor.is("MANAGER")) throw OrderProblem.forbidden();
    return refs
        .list(
            "select a.id,a.name,a.store_id,s.name store_name from store_delivery_areas a join"
                + " stores s on s.id=a.store_id order by a.store_id,a.id")
        .stream()
        .filter(a -> refs.operates(actor, CommerceReferenceRepository.number(a, "store_id")))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> deliveryRates(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN") && !actor.is("MANAGER")) throw OrderProblem.forbidden();
    return refs
        .list(
            "select r.additional_fee,r.is_active,c.name carrier_name,a.name"
                + " area_name,a.store_id,s.name store_name from carrier_area_rates r join carriers"
                + " c on c.id=r.carrier_id join store_delivery_areas a on a.id=r.delivery_area_id"
                + " join stores s on s.id=a.store_id order by a.store_id,a.id,r.carrier_id")
        .stream()
        .filter(a -> refs.operates(actor, CommerceReferenceRepository.number(a, "store_id")))
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> checkoutChoices(Actor actor) {
    support.buyer(actor);
    var cart = data.cart(actor.userId(), false);
    if (cart == null) throw OrderProblem.invalid("Giỏ hàng trống.");
    return Map.of(
        "addresses",
        refs.list(
            "select id,recipient_name,phone,address_detail from user_addresses where user_id=? order by"
                + " id",
            actor.userId()),
        "areas",
        refs.list(
            "select id,name from store_delivery_areas where store_id=? and is_active=true order by"
                + " id",
            cart.storeId),
        "carriers",
        data
            .query(Carrier.class, "where e.isActive=true and (e.managedStoreId is null or e.managedStoreId=:store) order by e.id", Map.of("store",cart.storeId), false, 100)
            .stream()
            .map(c -> carrierView(c,actor))
            .toList(),
        "promotions",
        refs.list(
            "select id,name,kind,code from promotions where is_active=true and (scope='GLOBAL' or"
                + " store_id=?) and starts_at<=? and ends_at>? order by id",
            cart.storeId,
            clock.instant(),
            clock.instant()));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> revenue(Actor actor, Long store, LocalDate from, LocalDate to) {
    refs.requireCurrent(actor, false);
    if (!actor.is("ADMIN") && !actor.is("MANAGER") && !actor.is("VENDOR"))
      throw OrderProblem.forbidden();
    if (store != null) refs.requireOperator(actor, store);
    if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusDays(365)))
      throw OrderProblem.invalid("Khoảng ngày báo cáo không hợp lệ; tối đa 366 ngày.");
    Instant start = from.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
        end = to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    Set<Long> allowed = new HashSet<>();
    for (var s : stores(actor)) allowed.add(CommerceReferenceRepository.number(s, "id"));
    if (store != null) allowed.retainAll(Set.of(store));
    BigDecimal goods = BigDecimal.ZERO,
        shipping = BigDecimal.ZERO,
        commission = BigDecimal.ZERO,
        refundGoods = BigDecimal.ZERO,
        refundShipping = BigDecimal.ZERO,
        reverse = BigDecimal.ZERO;
    long count = 0;
    for (var s :
        refs.list(
            "select s.* from store_settlements s join orders o on o.id=s.order_id where"
                + " o.delivered_at>=? and o.delivered_at<?",
            start,
            end)) {
      if (!allowed.contains(CommerceReferenceRepository.number(s, "store_id"))) continue;
      goods = goods.add(CommerceReferenceRepository.money(s, "goods_net"));
      shipping = shipping.add(CommerceReferenceRepository.money(s, "shipping_net"));
      commission = commission.add(CommerceReferenceRepository.money(s, "commission_amount"));
      count++;
    }
    for (var s :
        refs.list(
            "select s.* from refund_transactions r join store_settlements s on"
                + " s.order_id=r.order_id where r.status='SUCCEEDED' and r.completed_at>=? and"
                + " r.completed_at<?",
            start,
            end)) {
      if (!allowed.contains(CommerceReferenceRepository.number(s, "store_id"))) continue;
      refundGoods = refundGoods.add(CommerceReferenceRepository.money(s, "refunded_goods"));
      refundShipping =
          refundShipping.add(CommerceReferenceRepository.money(s, "refunded_shipping"));
      reverse = reverse.add(CommerceReferenceRepository.money(s, "commission_reversed"));
    }
    return Map.of(
        "from",
        from.toString(),
        "to",
        to.toString(),
        "orders",
        count,
        "goods",
        goods,
        "shipping",
        shipping,
        "commission",
        commission,
        "refund",
        refundGoods.add(refundShipping),
        "commissionReversed",
        reverse,
        "storeNet",
        goods.subtract(refundGoods).subtract(commission.subtract(reverse)));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> notifications(Actor actor, int page) {
    refs.requireCurrent(actor, false);
    if (page < 0 || page > 1000) throw OrderProblem.invalid("Trang không hợp lệ.");
    return data
        .query(
            Notification.class,
            "where e.userId=:user order by e.createdAt desc,e.id desc",
            Map.of("user", actor.userId()),
            false,
            (page + 1) * 20)
        .stream()
        .skip((long) page * 20)
        .map(
            n ->
                Map.<String, Object>of(
                    "id",
                    n.id,
                    "orderId",
                    n.orderId == null ? 0 : n.orderId,
                    "title",
                    n.title,
                    "content",
                    n.content,
                    "createdAt",
                    n.createdAt.toString(),
                    "read",
                    n.readAt != null))
        .toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void readNotification(Actor actor, long id) {
    refs.requireCurrent(actor, false);
    Notification n = data.require(Notification.class, id, true);
    if (n.userId != actor.userId()) throw OrderProblem.missing();
    if (n.readAt == null) n.readAt = clock.instant();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> shipperReport(Actor actor, LocalDate from, LocalDate to) {
    workflow.shipper(actor);
    if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusDays(365)))
      throw OrderProblem.invalid("Khoảng ngày không hợp lệ.");
    Instant start = from.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
        end = to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    var totals = refs.one("select count(*) assigned,"
        + " coalesce(sum(case when status='COMPLETED' then 1 else 0 end),0) completed,"
        + " coalesce(sum(case when status='ACTIVE' then 1 else 0 end),0) active,"
        + " coalesce(sum(case when status='CANCELLED' then 1 else 0 end),0) cancelled"
        + " from delivery_assignments where shipper_id=? and assigned_at>=? and assigned_at<?",
        actor.userId(),start,end);
    Map<String,Object> result = new LinkedHashMap<>();
    result.put("from",from.toString()); result.put("to",to.toString());
    for (String key : List.of("assigned","completed","active","cancelled"))
      result.put(key,CommerceReferenceRepository.number(totals,key));
    result.put("failed",CommerceReferenceRepository.number(refs.one(
        "select count(distinct assignment_id) n from delivery_events where actor_id=?"
        + " and type='FAILED' and created_at>=? and created_at<?",actor.userId(),start,end),"n"));
    result.put("codCollected",refs.one("select coalesce(sum(final_amount),0) amount from orders"
        + " where payment_method='COD' and payment_confirmed_by=? and paid_at>=? and paid_at<?",
        actor.userId(),start,end).get("amount"));
    return result;
  }
}
