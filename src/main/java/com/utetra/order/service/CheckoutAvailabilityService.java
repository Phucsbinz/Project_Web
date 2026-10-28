package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.utetra.order.repository.CommerceReferenceRepository.*;

@Service
public class CheckoutAvailabilityService {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final OrderSupport support;
  private final Clock clock;
  public CheckoutAvailabilityService(OrderDataRepository data,CommerceReferenceRepository refs,
      OrderSupport support,Clock clock) {
    this.data=data;this.refs=refs;this.support=support;this.clock=clock;
  }
  @Transactional(readOnly=true)
  public Map<String,Object> snapshot(Actor actor) {
    support.buyer(actor);
    var cart=data.cart(actor.userId(),false);
    Map<String,Object> result=new LinkedHashMap<>();
    result.put("storeId",cart==null?0:cart.storeId);
    List<Map<String,Object>> variants=new ArrayList<>(),promotions=new ArrayList<>();
    if(cart!=null) {
      Map<Long,Integer> requested=new TreeMap<>();
      for(var item:data.selectedItems(cart.id)) requested.merge(item.variantId,item.quantity,Math::addExact);
      for(var entry:requested.entrySet()) {
        var row=refs.one("select v.stock_quantity,v.size_name,p.name from product_variants v join products p on p.id=v.product_id where v.id=?",entry.getKey());
        Map<String,Object> v=new LinkedHashMap<>();v.put("id",entry.getKey());v.put("requested",entry.getValue());
        v.put("stockQuantity",row.get("stock_quantity"));v.put("name",row.get("name"));v.put("size",row.get("size_name"));
        variants.add(v);
      }
      for(var p:refs.list("select p.*, (select count(*) from coupon_redemptions c where c.promotion_id=p.id and c.status in ('RESERVED','USED')) used_count, (select count(*) from coupon_redemptions c where c.promotion_id=p.id and c.user_id=? and c.status in ('RESERVED','USED')) own_count from promotions p where (p.scope='GLOBAL' or p.store_id=?) and "+com.utetra.identity.OfferWalletService.VISIBLE+" order by p.id",actor.userId(),cart.storeId,actor.userId())) {
        long remaining=Math.max(0,number(p,"usage_limit")-number(p,"used_count"));
        long own=Math.max(0,number(p,"per_user_limit")-number(p,"own_count"));
        boolean available=flag(p,"is_active") && remaining>0 && own>0
            && !clock.instant().isBefore(instant(p.get("starts_at"))) && clock.instant().isBefore(instant(p.get("ends_at")));
        promotions.add(Map.of("id",number(p,"id"),"remaining",remaining,"ownRemaining",own,"available",available));
      }
    }
    result.put("variants",variants);result.put("promotions",promotions);
    result.put("revision",support.hash(result));
    return result;
  }
}
