package com.utetra.order.service;

import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Available cups per size. NULL means the shop has not enabled finite inventory. */
@Service
@Transactional(propagation=Propagation.MANDATORY)
public class OrderInventoryService {
  private final CommerceReferenceRepository refs;
  private final OrderDataRepository data;
  private final CheckoutAvailabilityEvents events;
  private final Clock clock;
  public OrderInventoryService(CommerceReferenceRepository refs,OrderDataRepository data,
      CheckoutAvailabilityEvents events,Clock clock) {
    this.refs=refs;this.data=data;this.events=events;this.clock=clock;
  }
  public Set<Long> reserve(long store,Map<Long,Integer> quantities) {
    Set<Long> tracked=new LinkedHashSet<>();
    for(var entry:new TreeMap<>(quantities).entrySet()) {
      var variant=refs.one("select stock_quantity from product_variants where id=? for update",entry.getKey());
      if(variant.get("stock_quantity")==null) continue;
      int changed=refs.write("update product_variants set stock_quantity=stock_quantity-?,version=version+1,updated_at=? where id=? and stock_quantity>=?",
          entry.getValue(),clock.instant(),entry.getKey(),entry.getValue());
      if(changed!=1) throw OrderProblem.conflict("Size đã hết hoặc không còn đủ số lượng. Hãy cập nhật giỏ hàng.");
      tracked.add(entry.getKey());
    }
    if(!tracked.isEmpty()) events.changed(store,tracked,List.of());
    return tracked;
  }
  public void check(Map<Long,Integer> quantities) {
    for(var entry:quantities.entrySet()) {
      var v=refs.one("select stock_quantity from product_variants where id=?",entry.getKey());
      if(v.get("stock_quantity")!=null && CommerceReferenceRepository.number(v,"stock_quantity")<entry.getValue())
        throw OrderProblem.conflict("Size đã hết hoặc không còn đủ số lượng. Hãy cập nhật giỏ hàng.");
    }
  }
  public void releaseBeforePreparation(Order order,String previousStatus) {
    if(!Set.of("NEW","CONFIRMED").contains(previousStatus)) return;
    Set<Long> changed=new LinkedHashSet<>();
    // Point UPDATEs acquire variant locks in the same order as checkout/reserve/branch selection.
    for(var item:data.items(order.id).stream()
        .sorted(Comparator.comparing((OrderItem i)->i.variantId).thenComparing(i->i.id)).toList()) {
      if(!item.stockReserved) continue;
      refs.write("update product_variants set stock_quantity=stock_quantity+?,version=version+1,updated_at=? where id=? and stock_quantity is not null",
          item.quantity,clock.instant(),item.variantId);
      item.stockReserved=false;
      changed.add(item.variantId);
    }
    if(!changed.isEmpty()) events.changed(order.storeId,changed,List.of());
  }
}
