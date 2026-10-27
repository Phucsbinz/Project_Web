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
    public String mergeKey = UUID.randomUUID().toString();
    public long version;
    public boolean draft;
    public java.time.Instant updatedAt;
    public final Set<String> unselected=new HashSet<>();
  }

  private final CartService carts;
  private final CommerceReferenceRepository refs;
  private final java.time.Clock clock;

  public GuestCartService(CartService carts, CommerceReferenceRepository refs,java.time.Clock clock) {
    this.carts = carts;
    this.refs = refs;this.clock=clock;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> view(Snapshot g) {
    synchronized (g) {
      expire(g);
      return Map.of(
          "serverNow",clock.instant().toString(),
          "expiresAt",g.updatedAt==null?"":g.updatedAt.plus(com.utetra.order.repository.OrderDataRepository.CART_TTL).toString(),
          "items",
          g.items.stream().map(item -> {var row=carts.itemView(carts.configuration(item), item);row.put("selected",!g.unselected.contains(carts.configuration(item)));return row;}).toList(),
          "version",
          g.version);
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> add(Snapshot g, Item raw) {
    return add(g,raw,false);
  }

  @Transactional(readOnly=true)
  public Map<String,Object> addDraft(Snapshot g,Item raw) { return add(g,raw,true); }

  private Map<String,Object> add(Snapshot g,Item raw,boolean draft) {
    Item item = draft?carts.validateDraft(raw):carts.validate(raw);
    synchronized (g) {
      expire(g);
      if (!draft && !g.items.isEmpty() && store(item.variantId()) != store(g.items.getFirst().variantId()))
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
      g.unselected.remove(hash);g.updatedAt=clock.instant();
      g.draft |= draft;
      g.version++;
      return view(g);
    }
  }

  public void expire(Snapshot g){synchronized(g){if(g.updatedAt==null&&!g.items.isEmpty())g.updatedAt=clock.instant();if(g.updatedAt!=null&&!g.updatedAt.plus(com.utetra.order.repository.OrderDataRepository.CART_TTL).isAfter(clock.instant())){g.items.clear();g.unselected.clear();g.draft=false;g.updatedAt=null;g.mergeKey=UUID.randomUUID().toString();g.version++;}}}
  public Map<String,Object> select(Snapshot g,Set<String> ids,long version){synchronized(g){expire(g);if(version!=g.version)throw OrderProblem.conflict("Giỏ đã đổi hoặc hết hạn.");var all=g.items.stream().map(carts::configuration).collect(java.util.stream.Collectors.toSet());if(!all.containsAll(ids))throw OrderProblem.invalid("Món không thuộc giỏ.");g.unselected.clear();all.stream().filter(id->!ids.contains(id)).forEach(g.unselected::add);g.updatedAt=clock.instant();g.version++;return view(g);}}

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
      expire(g);
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
      if(remove)g.unselected.remove(id);g.updatedAt=clock.instant();
      g.version++;
      return view(g);
    }
  }
}
