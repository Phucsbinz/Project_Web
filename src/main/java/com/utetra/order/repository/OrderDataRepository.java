package com.utetra.order.repository;

import com.utetra.order.entity.*;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class OrderDataRepository {
  @PersistenceContext private EntityManager em;
  private final java.time.Clock clock;
  public OrderDataRepository(java.time.Clock clock){this.clock=clock;}
  public static final java.time.Duration CART_TTL=java.time.Duration.ofHours(1);
  public java.time.Instant expiresAt(Cart cart){return cart.updatedAt.plus(CART_TTL);}


  public <T> T save(T value) {
    em.persist(value);
    return value;
  }

  public void flush() {
    em.flush();
  }

  public void remove(Object value) {
    em.remove(value);
  }

  public <T> T require(Class<T> type, long id, boolean lock) {
    T value = lock ? em.find(type, id, LockModeType.PESSIMISTIC_WRITE) : em.find(type, id);
    if (value == null) throw com.utetra.order.service.OrderProblem.missing();
    return value;
  }

  public <T> List<T> query(
      Class<T> type, String where, Map<String, ?> params, boolean lock, int limit) {
    var q = em.createQuery("select e from " + type.getSimpleName() + " e " + where, type);
    params.forEach(q::setParameter);
    q.setMaxResults(limit);
    if (lock) q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
    return q.getResultList();
  }

  public Cart cart(long userId, boolean lock) {
    Cart cart=query(Cart.class, "where e.userId=:user", Map.of("user", userId), lock, 1).stream().findFirst().orElse(null);
    if(cart!=null && !expiresAt(cart).isAfter(clock.instant())) {
      if(!lock)return null;
      if(cart.groupOrderId!=null){var group=require(GroupOrder.class,cart.groupOrderId,true);if(java.util.Set.of("OPEN","LOCKED").contains(group.status))group.status="EXPIRED";cart.groupOrderId=null;}
      clear(cart);em.flush();
    }
    return cart;
  }
  public List<CartItem> selectedItems(long cartId){return cartItems(cartId).stream().filter(i->i.selected).toList();}

  public List<CartItem> cartItems(long cartId) {
    return query(
        CartItem.class, "where e.cartId=:cart order by e.id", Map.of("cart", cartId), false, 100);
  }

  public List<CartItemTopping> cartToppings(long itemId) {
    return query(
        CartItemTopping.class,
        "where e.cartItemId=:item order by e.toppingId",
        Map.of("item", itemId),
        false,
        100);
  }

  public List<OrderItem> items(long orderId) {
    return query(
        OrderItem.class,
        "where e.orderId=:order order by e.id",
        Map.of("order", orderId),
        false,
        100);
  }

  public List<OrderItemTopping> toppings(long itemId) {
    return query(
        OrderItemTopping.class,
        "where e.orderItemId=:item order by e.toppingId",
        Map.of("item", itemId),
        false,
        100);
  }

  public Order checkout(long user, String key) {
    return query(
            Order.class,
            "where e.userId=:user and e.checkoutKey=:key",
            Map.of("user", user, "key", key),
            false,
            1)
        .stream()
        .findFirst()
        .orElse(null);
  }

  public void clear(Cart cart) {
    for (var line : cartItems(cart.id)) {
      for (var top : cartToppings(line.id)) remove(top);
      remove(line);
    }
    cart.updatedAt = clock.instant();cart.activityRevision++;
  }
}
