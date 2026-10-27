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

  public Item validate(Item item) { return validate(item,false); }

  public Item validateDraft(Item item) { return validate(item,true); }

  private Item validate(Item item,boolean draft) {
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
    var variant = draft ? refs.one("select v.id,p.id product_id,p.store_id from product_variants v join products p on p.id=v.product_id join categories c on c.id=p.category_id join stores s on s.id=p.store_id where v.id=? and v.is_active=true and p.is_active=true and c.is_active=true and s.status='ACTIVE'",item.variantId()) : refs.saleVariant(item.variantId(), false, clock);
    for (long top : tops) {
      if(draft) refs.one("select t.id from toppings t join product_toppings pt on pt.topping_id=t.id where t.id=? and pt.product_id=? and t.is_active=true",top,CommerceReferenceRepository.number(variant,"product_id"));
      else refs.topping(
          top,
          CommerceReferenceRepository.number(variant, "product_id"),
          CommerceReferenceRepository.number(variant, "store_id"),
          false);
    }
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

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String,Object> addDraft(Actor actor,Item raw) {
    support.buyer(actor); refs.user(actor.userId(),true);
    return addLocked(actor,validateDraft(raw),true);
  }

  private Map<String,Object> addLocked(Actor actor,Item item) { return addLocked(actor,item,false); }
  private Map<String, Object> addLocked(Actor actor, Item item,boolean draft) {return addLocked(actor,item,draft,true);}
  private Map<String, Object> addLocked(Actor actor, Item item,boolean draft,boolean render) {
    var variant = draft ? refs.one("select p.store_id from products p join product_variants v on v.product_id=p.id where v.id=?",item.variantId()) : refs.saleVariant(item.variantId(), true, clock);
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
    editable(cart);
    var lines = data.cartItems(cart.id);
    if (!draft && !cart.storeId.equals(store) && !lines.isEmpty())
      throw OrderProblem.conflict("Giỏ thuộc cửa hàng khác; hãy xóa hoặc xác nhận đổi cửa hàng.");
    if(!draft || lines.isEmpty()) cart.storeId = store;
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
      line.quantity += item.quantity();line.selected=true;
    }
    touch(cart);
    return render?view(actor):Map.of();
  }

  private void editable(Cart cart) { if(cart.groupOrderId!=null){var group=data.require(GroupOrder.class,cart.groupOrderId,false);if(java.util.Set.of("CANCELLED","EXPIRED").contains(group.status)||!group.expiresAt.isAfter(clock.instant()))cart.groupOrderId=null;else throw OrderProblem.conflict("Giỏ nhóm đã chốt. Mở đơn nhóm để xử lý trước khi sửa giỏ.");} }

  private void touch(Cart cart) {
    cart.updatedAt=clock.instant();cart.activityRevision++;data.flush();
  }

  public Map<String, Object> itemView(Object id, Item item) {
    var rows =
        refs.list(
            "select p.name product_name,p.image_url,p.base_price,v.extra_price,v.size_name from product_variants v join products p on"
                + " p.id=v.product_id where v.id=?",
            item.variantId());
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", id);
    view.put("variantId", item.variantId());
    view.put(
        "name", rows.isEmpty() ? "Món không còn khả dụng" : rows.getFirst().get("product_name"));
    view.put("size", rows.isEmpty() ? "" : rows.getFirst().get("size_name"));
    if(!rows.isEmpty()) {
      var source=rows.getFirst();view.put("imageUrl",source.get("image_url"));
      var unit=CommerceReferenceRepository.money(source,"base_price").add(CommerceReferenceRepository.money(source,"extra_price"));
      for(long top:item.toppingIds())unit=unit.add(CommerceReferenceRepository.money(refs.one("select price from toppings where id=?",top),"price"));
      view.put("unitPrice",unit);view.put("subtotal",unit.multiply(java.math.BigDecimal.valueOf(item.quantity())));
    }
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

  private Map<String,Object> selectedView(CartItem item,Map<String,Object> view){view.put("selected",item.selected);return view;}
  @Transactional(isolation=Isolation.READ_COMMITTED)
  public Map<String,Object> select(Actor actor,Set<Long> ids,long version){
    support.buyer(actor);refs.user(actor.userId(),true);var cart=data.cart(actor.userId(),true);
    if(cart==null||cart.version!=version)throw OrderProblem.conflict("Giỏ đã đổi hoặc hết hạn. Hãy tải lại.");editable(cart);
    var items=data.cartItems(cart.id);if(!items.stream().map(i->i.id).collect(java.util.stream.Collectors.toSet()).containsAll(ids))throw OrderProblem.invalid("Món được chọn không thuộc giỏ.");
    items.forEach(item->item.selected=ids.contains(item.id));touch(cart);return view(actor);
  }
  @Transactional
  public Map<String,Object> applyGuestExclusions(Actor actor,Set<String> unselected){
    support.buyer(actor);refs.user(actor.userId(),true);var cart=data.cart(actor.userId(),true);if(cart==null)return view(actor);editable(cart);
    for(var item:data.cartItems(cart.id))if(unselected.contains(item.configurationHash))item.selected=false;
    touch(cart);return view(actor);
  }
  @Transactional
  public void expireInactive(){
    for(var row:refs.list("select user_id from carts where updated_at<=? order by user_id limit 100",clock.instant().minus(com.utetra.order.repository.OrderDataRepository.CART_TTL))){
      long user=CommerceReferenceRepository.number(row,"user_id");refs.one("select id from users where id=? for update",user);data.cart(user,true);
    }
  }

  @Transactional(readOnly=true)
  public Map<String,Object> count(Actor actor) {
    support.buyer(actor);
    var rows=refs.one("select count(i.id) line_count,coalesce(sum(i.quantity),0) cups from carts c left join cart_items i on i.cart_id=c.id where c.user_id=? and c.updated_at>?",actor.userId(),clock.instant().minus(com.utetra.order.repository.OrderDataRepository.CART_TTL));
    return Map.of("count",rows.get("line_count"),"cups",rows.get("cups"));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> view(Actor actor) {
    support.buyer(actor);
    Cart cart = data.cart(actor.userId(), false);
    if (cart == null) return Map.of("items", List.of(), "version", 0,"expiresAt","");
    var items =
        data.cartItems(cart.id).stream()
            .map(
                l ->
                    selectedView(l,
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
                            l.note))))
            .toList();
    return Map.of("storeId", cart.storeId, "version", cart.version, "items", items,"groupOrderId",cart.groupOrderId==null?0:cart.groupOrderId,"expiresAt",data.expiresAt(cart).toString(),"serverNow",clock.instant().toString());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> change(
      Actor actor, long itemId, int quantity, long version, boolean remove) {
    support.buyer(actor);
    refs.user(actor.userId(), true);
    Cart cart = data.cart(actor.userId(), true);
    if (cart == null) throw OrderProblem.missing();
    if (cart.version != version) throw OrderProblem.conflict("Giỏ đã thay đổi, hãy tải lại.");
    editable(cart);
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
      editable(cart);
      data.clear(cart);
      touch(cart);
    }
    return view(actor);
  }

  @Transactional(isolation=Isolation.READ_COMMITTED)
  public void replaceDraftForGroup(Actor actor,List<Item> items) {
    support.buyer(actor);refs.user(actor.userId(),true);clear(actor);
    for(var item:items)addLocked(actor,validateDraft(item),true,false);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String,Object> selectBranch(Actor actor,long storeId,long expectedVersion) {
    support.buyer(actor); refs.user(actor.userId(),true);
    if(storeId<=0 || expectedVersion<0)throw OrderProblem.invalid("Chi nhánh hoặc phiên bản giỏ không hợp lệ.");
    var cart=data.cart(actor.userId(),true);
    if(cart==null || data.selectedItems(cart.id).isEmpty())throw OrderProblem.invalid("Hãy tick chọn ít nhất một món trước khi thanh toán.");
    if(cart.version!=expectedVersion)throw OrderProblem.conflict("Giỏ đã thay đổi. Vui lòng tải lại trước khi chọn chi nhánh.");
    refs.store(storeId,true);
    List<Item> mapped=new ArrayList<>();Map<Long,Integer> quantities=new TreeMap<>();
    for(var line:data.selectedItems(cart.id)) {
      var candidates=refs.list("select v.id,p.name from product_variants source join products original on original.id=source.product_id join stores os on os.id=original.store_id join stores target on target.id=? and target.owner_user_id=os.owner_user_id join products p on p.store_id=target.id and p.category_id=original.category_id and (p.id=original.id or (original.catalog_key is not null and p.catalog_key=original.catalog_key)) join product_variants v on v.product_id=p.id and v.size_name=source.size_name where source.id=? and p.is_active=true and p.is_available=true and v.is_active=true order by p.id,v.id",storeId,line.variantId);
      if(candidates.size()!=1)throw OrderProblem.conflict("Chi nhánh chưa phục vụ đủ món hoặc size đã chọn. Vui lòng chọn chi nhánh khác hoặc sửa giỏ.");
      long variant=CommerceReferenceRepository.number(candidates.getFirst(),"id");
      var tops=data.cartToppings(line.id).stream().map(t->t.toppingId).toList();
      Item item=validate(new Item(variant,line.sugarLevel,line.iceLevel,tops,line.quantity,line.note));
      mapped.add(item);quantities.merge(variant,line.quantity,Math::addExact);
    }
    for(var entry:quantities.entrySet()) {
      refs.saleVariant(entry.getKey(),true,clock);
      var v=refs.one("select stock_quantity from product_variants where id=? for update",entry.getKey());
      if(v.get("stock_quantity")!=null && CommerceReferenceRepository.number(v,"stock_quantity")<entry.getValue())throw OrderProblem.conflict("Chi nhánh không còn đủ số lượng cho size đã chọn.");
    }
    // Validate the full replacement before deleting anything; transaction rollback preserves the original cart.
    var groupId=cart.groupOrderId;cart.groupOrderId=null;
    Set<String> skipped=data.cartItems(cart.id).stream().filter(i->!i.selected).map(i->i.configurationHash).collect(java.util.stream.Collectors.toSet());
    if(mapped.stream().map(this::configuration).anyMatch(skipped::contains))throw OrderProblem.conflict("Món được chọn trùng cấu hình với món chưa chọn tại chi nhánh này. Hãy chọn cả hai dòng hoặc đổi chi nhánh.");
    for(var line:data.selectedItems(cart.id)){for(var top:data.cartToppings(line.id))data.remove(top);data.remove(line);}
    data.flush();cart.storeId=storeId;
    for(Item item:mapped)addLocked(actor,item,false,false);
    cart.groupOrderId=groupId;touch(cart);return view(actor);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String,Object> mergeDraft(Actor actor,List<Item> guest,String mergeKey) {
    support.buyer(actor);refs.user(actor.userId(),true);UUID.fromString(mergeKey);
    var cart=data.cart(actor.userId(),true);
    if(cart!=null && mergeKey.equals(cart.lastMergeKey))return view(actor);
    for(var item:guest)addLocked(actor,validateDraft(item),true);
    cart=data.cart(actor.userId(),true);if(cart!=null){cart.lastMergeKey=mergeKey;touch(cart);}
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
