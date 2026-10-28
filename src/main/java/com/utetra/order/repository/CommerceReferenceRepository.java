package com.utetra.order.repository;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.service.OrderProblem;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Cross-module data access against the approved shared schema; no catalog/identity CRUD. */
@Repository
public class CommerceReferenceRepository {
  private final JdbcTemplate jdbc;

  public CommerceReferenceRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> one(String sql, Object... args) {
    var rows = jdbc.queryForList(sql, args);
    if (rows.isEmpty()) throw OrderProblem.missing();
    return rows.getFirst();
  }

  public List<Map<String, Object>> list(String sql, Object... args) {
    return jdbc.queryForList(sql, args);
  }

  public int write(String sql, Object... args) {
    return jdbc.update(sql, args);
  }

  public static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }

  public static boolean flag(Map<String, Object> row, String key) {
    Object v = row.get(key);
    return Boolean.TRUE.equals(v) || v instanceof Number n && n.intValue() != 0;
  }

  public static BigDecimal money(Map<String, Object> row, String key) {
    return new BigDecimal(row.get(key).toString());
  }

  public static Instant instant(Object value) {
    if (value instanceof java.time.OffsetDateTime d) return d.toInstant();
    if (value instanceof java.sql.Timestamp t) return t.toInstant();
    if (value instanceof LocalDateTime d) return d.toInstant(ZoneOffset.UTC);
    if (value instanceof Instant i) return i;
    throw OrderProblem.invalid("Thiếu thời điểm cấu hình.");
  }

  public Map<String, Object> user(long userId, boolean lock) {
    // Match identity's lock contract: lock the user row, not the shared roles row.
    if (lock) one("select id from users where id=? for update", userId);
    var row =
        one(
            "select u.id,u.is_active,u.email_verified_at,r.name role_name from users u join roles r"
                + " on r.id=u.role_id where u.id=?",
            userId);
    if (!flag(row, "is_active") || row.get("email_verified_at") == null)
      throw OrderProblem.forbidden();
    return row;
  }

  public void requireCurrent(Actor actor, boolean lock) {
    if (actor == null || actor.userId() <= 0) throw OrderProblem.forbidden();
    if (!actor.role().equals(user(actor.userId(), lock).get("role_name")))
      throw OrderProblem.forbidden();
  }

  public Map<String, Object> store(long id, boolean lock) {
    return one("select * from stores where id=?" + (lock ? " for update" : ""), id);
  }

  public boolean operates(Actor actor, long storeId) {
    if (actor.is("ADMIN")) return true;
    var store = store(storeId, false);
    if (actor.is("VENDOR")) return number(store, "owner_user_id") == actor.userId();
    if (actor.is("MANAGER"))
      return !list(
              "select id from store_memberships where user_id=? and store_id=? and is_active=true"
                  + " and member_role in ('MANAGER','CATALOG_ADMIN')",
              actor.userId(),
              storeId)
          .isEmpty();
    return false;
  }

  public void requireOperator(Actor actor, long storeId) {
    requireCurrent(actor, false);
    if (!operates(actor, storeId)) throw OrderProblem.forbidden();
  }

  /** Call before order/store locks. Membership revocation takes the same user lock. */
  public void requireOperatorMutation(Actor actor, long storeId) {
    if(actor==null) throw OrderProblem.forbidden();
    var u=one("select role_id,is_active,email_verified_at from users where id=? for update",actor.userId());
    var role=one("select name from roles where id=?",u.get("role_id")).get("name");
    if(!flag(u,"is_active") || u.get("email_verified_at")==null || !actor.role().equals(role)) throw OrderProblem.forbidden();
    if(actor.is("ADMIN")) return;
    if(actor.is("VENDOR") && number(store(storeId,false),"owner_user_id")==actor.userId()) return;
    if(actor.is("MANAGER") && !list("select id from store_memberships where user_id=? and store_id=? and is_active=true and member_role in ('MANAGER','CATALOG_ADMIN') for update",actor.userId(),storeId).isEmpty()) return;
    throw OrderProblem.forbidden();
  }

  public Map<String, Object> saleVariant(long variantId, boolean lock, Clock clock) {
    String sql =
        "select v.id variant_id,v.product_id,v.size_name,v.extra_price,p.store_id,p.name"
            + " product_name,p.base_price,p.is_active product_active,p.is_available,"
            + " (select c.is_active from categories c where c.id=p.category_id) category_active,v.is_active"
            + " variant_active from product_variants v join products p on p.id=v.product_id where"
            + " v.id=?";
    var v = one(sql, variantId);
    long storeId = number(v, "store_id");
    // Every order operation locks store before catalog rows to avoid cross-user lock inversion.
    var s = store(storeId, lock);
    if (lock) {
      v = one(sql + " for update", variantId);
      if (number(v, "store_id") != storeId)
        throw OrderProblem.conflict("Món đã chuyển cấu hình cửa hàng; hãy tải lại.");
    }
    if (!flag(v, "product_active")
        || !flag(v, "category_active")
        || !flag(v, "variant_active")
        || !flag(v, "is_available")
        || !"ACTIVE".equals(s.get("status"))
        || !flag(s, "accepting_orders"))
      throw OrderProblem.conflict("Món hoặc cửa hàng đang ngừng nhận đơn.");
    LocalTime now = clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime();
    LocalTime open = LocalTime.parse(s.get("open_time").toString()),
        close = LocalTime.parse(s.get("close_time").toString());
    if (now.isBefore(open) || !now.isBefore(close))
      throw OrderProblem.conflict("Cửa hàng ngoài giờ nhận đơn.");
    return v;
  }

  public Map<String, Object> topping(long id, long product, long store, boolean lock) {
    var row =
        one(
            "select t.* from toppings t join product_toppings pt on pt.topping_id=t.id join"
                + " store_toppings st on st.topping_id=t.id where t.id=? and pt.product_id=? and"
                + " st.store_id=? and t.is_active=true and st.is_available=true"
                + (lock ? " for update" : ""),
            id,
            product,
            store);
    if (money(row, "price").signum() < 0) throw OrderProblem.invalid("Giá topping không hợp lệ.");
    return row;
  }

  public Map<String, Object> address(long actor, long id) {
    return one("select * from user_addresses where id=? and user_id=?", id, actor);
  }

  public Map<String, Object> area(long store, long id) {
    return one(
        "select * from store_delivery_areas where id=? and store_id=? and is_active=true",
        id,
        store);
  }

  public List<Long> recipients(long store) {
    Set<Long> ids = new LinkedHashSet<>();
    ids.add(number(store(store, false), "owner_user_id"));
    for (var r :
        list(
            "select user_id from store_memberships where store_id=? and is_active=true and"
                + " member_role in ('MANAGER','CATALOG_ADMIN')",
            store)) ids.add(number(r, "user_id"));
    return List.copyOf(ids);
  }
}
