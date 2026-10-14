package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final OrderSupport support;
  private final Clock clock;

  public CartService(
      OrderDataRepository data,
      CommerceReferenceRepository refs,
      OrderSupport support,
      Clock clock) {
    this.data = data;
    this.refs = refs;
    this.support = support;
    this.clock = clock;
  }

  public Item validate(Item item) {
    if (item == null
        || item.variantId() <= 0
        || item.quantity() < 1
        || item.quantity() > 20
        || !Set.of(0, 30, 50, 70, 100).contains(item.sugar())
        || !Set.of(0, 30, 50, 70, 100).contains(item.ice()))
      throw OrderProblem.invalid("Tùy chọn hoặc số lượng món không hợp lệ.");
    if (item.toppingIds() != null && item.toppingIds().stream().anyMatch(t -> t == null || t <= 0))
      throw OrderProblem.invalid("Topping không hợp lệ.");
    var tops =
        item.toppingIds() == null
            ? List.<Long>of()
            : item.toppingIds().stream().distinct().sorted().toList();
    if (tops.size() > 20 || tops.stream().anyMatch(t -> t == null || t <= 0))
      throw OrderProblem.invalid("Topping không hợp lệ.");
    var variant = refs.saleVariant(item.variantId(), false, clock);
    for (long top : tops)
      refs.topping(
          top,
          CommerceReferenceRepository.number(variant, "product_id"),
          CommerceReferenceRepository.number(variant, "store_id"),
          false);
    return new Item(
        item.variantId(),
        item.sugar(),
        item.ice(),
        tops,
        item.quantity(),
        OrderSupport.note(item.note(), 255));
  }

  public String configuration(Item i) {
    return support.hash(List.of(i.variantId(), i.sugar(), i.ice(), i.toppingIds(), i.note()));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> add(Actor actor, Item raw) {
    support.buyer(actor);
    refs.user(actor.userId(), true);
    return addLocked(actor, validate(raw));
  }

  private Map<String, Object> addLocked(Actor actor, Item item) {
    var variant = refs.saleVariant(item.variantId(), true, clock);
    long store = CommerceReferenceRepository.number(variant, "store_id");
    Cart cart = data.cart(actor.userId(), true);
    if (cart == null) {
      cart = new Cart();
      cart.userId = actor.userId();
      cart.storeId = store;
      cart.createdAt = clock.instant();
      cart.updatedAt = clock.instant();
      data.save(cart);
      data.flush();
    }
    var lines = data.cartItems(cart.id);
    if (!cart.storeId.equals(store) && !lines.isEmpty())
      throw OrderProblem.conflict("Giỏ thuộc cửa hàng khác; hãy xóa hoặc xác nhận đổi cửa hàng.");
    cart.storeId = store;
    int total = lines.stream().mapToInt(l -> l.quantity).sum();
    if (total + item.quantity() > 50) throw OrderProblem.invalid("Mỗi đơn tối đa 50 ly.");
    String hash = configuration(item);
    CartItem line =
        lines.stream().filter(l -> l.configurationHash.equals(hash)).findFirst().orElse(null);
    if (line == null) {
      line = new CartItem();
      line.cartId = cart.id;
      line.variantId = item.variantId();
      line.sugarLevel = item.sugar();
      line.iceLevel = item.ice();
      line.quantity = item.quantity();
      line.note = item.note();
      line.configurationHash = hash;
      data.save(line);
      data.flush();
      for (long top : item.toppingIds()) {
        CartItemTopping t = new CartItemTopping();
        t.cartItemId = line.id;
        t.toppingId = top;
        data.save(t);
      }
    } else {
      if (line.quantity + item.quantity() > 20)
        throw OrderProblem.invalid("Mỗi dòng tối đa 20 ly.");
      line.quantity += item.quantity();
    }
    touch(cart);
    return view(actor);
  }

  private void touch(Cart cart) {
    cart.updatedAt =
        cart.updatedAt == null
            ? clock.instant()
            : clock.instant().isAfter(cart.updatedAt)
                ? clock.instant()
                : cart.updatedAt.plusNanos(1000);
    data.flush();
  }

  public Map<String, Object> itemView(Object id, Item item) {
    var rows =
        refs.list(
            "select p.name product_name,v.size_name from product_variants v join products p on"
                + " p.id=v.product_id where v.id=?",
            item.variantId());
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", id);
    view.put("variantId", item.variantId());
    view.put(
        "name", rows.isEmpty() ? "Món không còn khả dụng" : rows.getFirst().get("product_name"));
    view.put("size", rows.isEmpty() ? "" : rows.getFirst().get("size_name"));
    view.put("quantity", item.quantity());
    view.put("sugar", item.sugar());
    view.put("ice", item.ice());
    view.put("note", item.note());
    view.put("toppingIds", item.toppingIds());
    view.put(
        "toppings",
        item.toppingIds().stream()
            .map(
                top -> {
                  var names = refs.list("select name from toppings where id=?", top);
                  return names.isEmpty()
                      ? "Topping không còn khả dụng"
                      : names.getFirst().get("name").toString();
                })
            .toList());
    return view;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> view(Actor actor) {
    support.buyer(actor);
    Cart cart = data.cart(actor.userId(), false);
    if (cart == null) return Map.of("items", List.of(), "version", 0);
    var items =
        data.cartItems(cart.id).stream()
            .map(
                l ->
                    itemView(
                        l.id,
                        new Item(
                            l.variantId,
                            l.sugarLevel,
                            l.iceLevel,
                            data.cartToppings(l.id).stream()
                                .map(topping -> topping.toppingId)
                                .toList(),
                            l.quantity,
                            l.note)))
            .toList();
    return Map.of("storeId", cart.storeId, "version", cart.version, "items", items);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> change(
      Actor actor, long itemId, int quantity, long version, boolean remove) {
    support.buyer(actor);
    refs.user(actor.userId(), true);
    Cart cart = data.cart(actor.userId(), true);
    if (cart == null) throw OrderProblem.missing();
    if (cart.version != version) throw OrderProblem.conflict("Giỏ đã thay đổi, hãy tải lại.");
    CartItem item = data.require(CartItem.class, itemId, false);
    if (!item.cartId.equals(cart.id)) throw OrderProblem.missing();
    if (remove) {
      for (var t : data.cartToppings(item.id)) data.remove(t);
      data.remove(item);
    } else {
      if (quantity < 1
          || quantity > 20
          || data.cartItems(cart.id).stream().mapToInt(l -> l.quantity).sum()
                  - item.quantity
                  + quantity
              > 50) throw OrderProblem.invalid("Số lượng vượt giới hạn.");
      item.quantity = quantity;
    }
    touch(cart);
    return view(actor);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> clear(Actor actor) {
    support.buyer(actor);
    refs.user(actor.userId(), true);
    Cart cart = data.cart(actor.userId(), true);
    if (cart != null) {
      data.clear(cart);
      touch(cart);
    }
    return view(actor);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> merge(Actor actor, List<Item> guest, String mergeKey) {
    support.buyer(actor);
    refs.user(actor.userId(), true);
    UUID.fromString(mergeKey);
    Cart cart = data.cart(actor.userId(), true);
    if (cart != null && mergeKey.equals(cart.lastMergeKey)) return view(actor);
    for (var item : guest) addLocked(actor, validate(item));
    cart = data.cart(actor.userId(), true);
    if (cart != null) {
      cart.lastMergeKey = mergeKey;
      cart.updatedAt = clock.instant();
      data.flush();
    }
    return view(actor);
  }
}
