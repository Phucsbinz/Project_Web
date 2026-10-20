package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.Item;
import com.utetra.order.repository.CommerceReferenceRepository;
import java.io.Serializable;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestCartService {
  public static final class Snapshot implements Serializable {
    public final List<Item> items = new ArrayList<>();
    public final String mergeKey = UUID.randomUUID().toString();
    public long version;
  }

  private final CartService carts;
  private final CommerceReferenceRepository refs;

  public GuestCartService(CartService carts, CommerceReferenceRepository refs) {
    this.carts = carts;
    this.refs = refs;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> view(Snapshot g) {
    synchronized (g) {
      return Map.of(
          "items",
          g.items.stream().map(item -> carts.itemView(carts.configuration(item), item)).toList(),
          "version",
          g.version);
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> add(Snapshot g, Item raw) {
    Item item = carts.validate(raw);
    synchronized (g) {
      if (!g.items.isEmpty() && store(item.variantId()) != store(g.items.getFirst().variantId()))
        throw OrderProblem.conflict(
            "Giỏ thuộc cửa hàng khác; xác nhận xóa giỏ trước khi đổi shop.");
      if (g.items.stream().mapToInt(Item::quantity).sum() + item.quantity() > 50)
        throw OrderProblem.invalid("Giỏ tối đa 50 ly.");
      String hash = carts.configuration(item);
      int index = index(g, hash, false);
      if (index >= 0) {
        Item old = g.items.get(index);
        if (old.quantity() + item.quantity() > 20)
          throw OrderProblem.invalid("Mỗi dòng tối đa 20 ly.");
        g.items.set(
            index,
            new Item(
                old.variantId(),
                old.sugar(),
                old.ice(),
                old.toppingIds(),
                old.quantity() + item.quantity(),
                old.note()));
      } else g.items.add(item);
      g.version++;
      return view(g);
    }
  }

  private long store(long variant) {
    return CommerceReferenceRepository.number(
        refs.one(
            "select store_id from products p join product_variants v on v.product_id=p.id where"
                + " v.id=?",
            variant),
        "store_id");
  }

  private int index(Snapshot g, String id, boolean required) {
    for (int i = 0; i < g.items.size(); i++)
      if (carts.configuration(g.items.get(i)).equals(id)) return i;
    if (required) throw OrderProblem.missing();
    return -1;
  }

  public Map<String, Object> change(
      Snapshot g, String id, int quantity, long version, boolean remove) {
    synchronized (g) {
      if (g.version != version) throw OrderProblem.conflict("Giỏ đã thay đổi.");
      int index = index(g, id, true);
      Item old = g.items.get(index);
      if (remove) g.items.remove(index);
      else {
        if (quantity < 1
            || quantity > 20
            || g.items.stream().mapToInt(Item::quantity).sum() - old.quantity() + quantity > 50)
          throw OrderProblem.invalid("Số lượng vượt giới hạn.");
        g.items.set(
            index,
            new Item(
                old.variantId(), old.sugar(), old.ice(), old.toppingIds(), quantity, old.note()));
      }
      g.version++;
      return view(g);
    }
  }
}
