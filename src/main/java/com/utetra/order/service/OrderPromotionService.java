package com.utetra.order.service;

import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Promotion eligibility reads catalog-owned programs; order-owned redemptions share checkout locks.
 */
@Service
public class OrderPromotionService {
  private final CommerceReferenceRepository refs;
  private final OrderDataRepository data;
  private final Clock clock;
  private final CheckoutAvailabilityEvents events;

  public OrderPromotionService(
      CommerceReferenceRepository refs, OrderDataRepository data, Clock clock, CheckoutAvailabilityEvents events) {
    this.refs = refs;
    this.data = data;
    this.clock = clock;
    this.events = events;
  }

  public BigDecimal discount(
      Long id,
      long user,
      long store,
      String kind,
      BigDecimal subtotal,
      BigDecimal base,
      Map<Long, BigDecimal> products,
      boolean lock) {
    if (id == null) return BigDecimal.ZERO;
    var p = refs.one("select * from promotions where id=?" + (lock ? " for update" : ""), id);
    if (!refs.list("select id from promotion_wallet where promotion_id=? and source not like 'SAVED:%' and user_id<>?",id,user).isEmpty())
      throw OrderProblem.conflict("Ưu đãi này thuộc tài khoản khác.");
    Instant now = clock.instant();
    if (!CommerceReferenceRepository.flag(p, "is_active")
        || !kind.equals(p.get("kind"))
        || !("GLOBAL".equals(p.get("scope"))
            || "SHOP".equals(p.get("scope"))
                && p.get("store_id") != null
                && CommerceReferenceRepository.number(p, "store_id") == store)
        || now.isBefore(CommerceReferenceRepository.instant(p.get("starts_at")))
        || !now.isBefore(CommerceReferenceRepository.instant(p.get("ends_at")))
        || subtotal.compareTo(CommerceReferenceRepository.money(p, "min_order")) < 0)
      throw OrderProblem.conflict("Khuyến mãi không áp dụng cho đơn này.");
    // MySQL REPEATABLE READ: quota checks must use a current locking read after
    // the program lock, rather than a snapshot COUNT from before that lock.
    long used =
        lock
            ? refs.list(
                    "select id from coupon_redemptions where promotion_id=? and status in"
                        + " ('RESERVED','USED') for update",
                    id)
                .size()
            : CommerceReferenceRepository.number(
                refs.one(
                    "select count(*) n from coupon_redemptions where promotion_id=? and status in"
                        + " ('RESERVED','USED')",
                    id),
                "n");
    long own =
        lock
            ? refs.list(
                    "select id from coupon_redemptions where promotion_id=? and user_id=? and"
                        + " status in ('RESERVED','USED') for update",
                    id,
                    user)
                .size()
            : CommerceReferenceRepository.number(
                refs.one(
                    "select count(*) n from coupon_redemptions where promotion_id=? and user_id=?"
                        + " and status in ('RESERVED','USED')",
                    id,
                    user),
                "n");
    if (used >= CommerceReferenceRepository.number(p, "usage_limit")
        || own >= CommerceReferenceRepository.number(p, "per_user_limit"))
      throw OrderProblem.conflict("Mã giảm giá đã hết lượt sử dụng.");
    var links = refs.list("select product_id from promotion_products where promotion_id=?", id);
    if (kind.equals("PRODUCT_PERCENT") && !links.isEmpty()) {
      Set<Long> eligible = new HashSet<>();
      for (var l : links) eligible.add(CommerceReferenceRepository.number(l, "product_id"));
      base =
          products.entrySet().stream()
              .filter(e -> eligible.contains(e.getKey()))
              .map(Map.Entry::getValue)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    BigDecimal percent = CommerceReferenceRepository.money(p, "percent");
    BigDecimal maximum = OrderSupport.amount(CommerceReferenceRepository.money(p, "max_discount"));
    if (percent.signum() <= 0 || percent.compareTo(new BigDecimal("100")) > 0)
      throw OrderProblem.invalid("Tỷ lệ giảm không hợp lệ.");
    return base.multiply(percent)
        .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
        .min(maximum)
        .min(base);
  }

  public void reserve(Long id, long order, long user, BigDecimal discount) {
    if (id == null) return;
    CouponRedemption r = new CouponRedemption();
    r.promotionId = id;
    r.orderId = order;
    r.userId = user;
    r.status = "RESERVED";
    r.discountSnapshot = discount;
    var promotion =
        refs.one("select name,kind,funded_by from promotions where id=? for update", id);
    r.fundedBySnapshot = promotion.get("funded_by").toString();
    if (!java.util.Set.of("APP", "SHOP").contains(r.fundedBySnapshot))
      throw OrderProblem.invalid("Nguồn tài trợ khuyến mãi không hợp lệ.");
    r.promotionKindSnapshot = promotion.get("kind").toString();
    r.promotionNameSnapshot = promotion.get("name").toString();
    r.createdAt = clock.instant();
    data.save(r);
    events.changed(null,List.of(),List.of(id));
  }

  public void status(long order, String status) {
    for (var r :
        data.query(
            CouponRedemption.class, "where e.orderId=:order", Map.of("order", order), true, 10)) {
      r.status = status;
      if (status.equals("RELEASED")) r.releasedAt = clock.instant();
      if (status.equals("RELEASED")) events.changed(null,List.of(),List.of(r.promotionId));
    }
  }
}
