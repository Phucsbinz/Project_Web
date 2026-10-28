package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ShipperScopeService {
 private final CommerceReferenceRepository refs;
 private final OrderDataRepository data;
 private final Clock clock;
 private final boolean enabled;
 public ShipperScopeService(CommerceReferenceRepository refs,OrderDataRepository data,Clock clock,@Value("${features.shipper.enabled:false}") boolean enabled){this.refs=refs;this.data=data;this.clock=clock;this.enabled=enabled;}
 private void admin(Actor actor){refs.requireCurrent(actor,false);if(!enabled||!actor.is("ADMIN"))throw OrderProblem.forbidden();}
 /** All mutations lock involved users in stable order before any order/scope row. */
 public void lockUsers(long... users){
  Arrays.stream(users).distinct().sorted().forEach(id->refs.one("select id from users where id=? for update",id));
 }
 public boolean serves(long shipper,long store){
  return !refs.list("select sc.id from shipper_store_scopes sc join stores s on s.id=sc.store_id where sc.shipper_id=? and sc.store_id=? and sc.is_active=true and s.status='ACTIVE'",shipper,store).isEmpty();
 }
 public void requireService(long shipper,long store){
  if(!enabled||!"ROLE_SHIPPER".equals(refs.user(shipper,false).get("role_name"))||!serves(shipper,store))throw OrderProblem.forbidden();
 }
 @Transactional(readOnly=true)
 public Map<String,Object> choices(Actor actor){admin(actor);return Map.of("shippers",refs.list("select u.id,u.full_name from users u join roles r on r.id=u.role_id where r.name='ROLE_SHIPPER' and u.is_active=true and u.email_verified_at is not null order by u.id"),"stores",refs.list("select id,name from stores where status='ACTIVE' order by id"),"scopes",refs.list("select sc.id,sc.shipper_id,sc.store_id,sc.is_active,sc.version,u.full_name,s.name store_name from shipper_store_scopes sc join users u on u.id=sc.shipper_id join stores s on s.id=sc.store_id order by sc.store_id,sc.shipper_id"));}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public void set(Actor actor,long shipper,long store,boolean active,long version,String reason){
  lockUsers(actor.userId(),shipper);admin(actor);
  if(active&&(!"ROLE_SHIPPER".equals(refs.user(shipper,false).get("role_name"))||!"ACTIVE".equals(refs.store(store,false).get("status"))))throw OrderProblem.forbidden();
  var scopes=data.query(ShipperStoreScope.class,"where e.shipperId=:shipper and e.storeId=:store",Map.of("shipper",shipper,"store",store),true,1);
  var scope=scopes.isEmpty()?new ShipperStoreScope():scopes.getFirst();
  if(scope.version!=version)throw OrderProblem.conflict("Phạm vi đã thay đổi; hãy tải lại.");
  String detail=OrderSupport.required(reason,500);
  if(scope.id!=null&&scope.active==active)return;
  scope.shipperId=shipper;scope.storeId=store;scope.active=active;scope.updatedAt=clock.instant();
  if(scope.id==null)data.save(scope);
  ShipperScopeEvent ev=new ShipperScopeEvent();ev.shipperId=shipper;ev.storeId=store;ev.actorId=actor.userId();ev.active=active;ev.reason=detail;ev.createdAt=clock.instant();data.save(ev);data.flush();
 }
}
