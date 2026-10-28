package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import com.utetra.identity.entity.PromotionWallet;
import com.utetra.entity.catalog.Promotion;
import com.utetra.security.UtetraPrincipal;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class OfferWalletService {
 /** Bind viewer user id. Public programs remain available; personal gifts never leak. */
 public static final String VISIBLE="not exists(select 1 from promotion_wallet private_wallet where private_wallet.promotion_id=p.id and private_wallet.source not like 'SAVED:%' and private_wallet.user_id<>?)";
 private final IdentityRepository db;private final AccountService account;private final EntityManager em;private final Clock clock;
 public OfferWalletService(IdentityRepository db,AccountService account,EntityManager em,Clock clock){this.db=db;this.account=account;this.em=em;this.clock=clock;}
 @Transactional(isolation=Isolation.READ_COMMITTED) public List<Map<String,Object>> sync(UtetraPrincipal p){account.require(p,true);award(p.userId());return list(p);}
 @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED)
 public void awardForOrder(long id){
  var order=db.optional("select user_id,group_order_id from orders where id=? and status='DELIVERED' and payment_status='PAID'",id);if(order.isEmpty())return;
  var users=new TreeSet<Long>();users.add(number(order.get(),"user_id"));
  for(var member:db.list("select user_id from group_order_members where order_id=? and billed_subtotal>0",id))users.add(number(member,"user_id"));
  // Delivery already committed: acquire all participant locks in stable order, independent of owner.
  for(long user:users)award(user);
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public void award(long user){
  db.user(user,true);
  long earned=db.optional("select lifetime_earned from loyalty_accounts where user_id=?",user).map(a->number(a,"lifetime_earned")).orElse(0L);
  if(earned>=100)grant(user,"TIER_SILVER","Quà hạng Bạc","PRODUCT_PERCENT",10,30000);
  if(earned>=500)grant(user,"TIER_GOLD","Quà hạng Vàng","PRODUCT_PERCENT",15,50000);
  long delivered=db.count("select count(*) from orders o where o.status='DELIVERED' and o.payment_status='PAID' and (o.user_id=? or exists(select 1 from group_order_members gm where gm.group_order_id=o.group_order_id and gm.user_id=? and gm.billed_subtotal>0))",user,user);
  for(long milestone=5;milestone<=delivered;milestone+=5)grant(user,"ORDERS_"+milestone,"Quà "+milestone+" đơn hàng","SHIPPING_PERCENT",100,15000);
 }
 private void grant(long user,String source,String name,String kind,int percent,int maximum){
  if(db.count("select count(*) from promotion_wallet where user_id=? and source=?",user,source)>0)return;
  var promotion=new Promotion();promotion.setCreatedBy(db.optional("select u.id from users u join roles r on r.id=u.role_id where r.name='ROLE_ADMIN' order by u.id limit 1").map(a->number(a,"id")).orElse(user));
  promotion.setScope("GLOBAL");promotion.setKind(kind);promotion.setCode("UT"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT));promotion.setName(name);promotion.setPercent(BigDecimal.valueOf(percent));promotion.setMaxDiscount(BigDecimal.valueOf(maximum));promotion.setMinOrder(BigDecimal.ZERO);promotion.setUsageLimit(1);promotion.setPerUserLimit(1);promotion.setStartsAt(clock.instant());promotion.setEndsAt(clock.instant().plus(Duration.ofDays(30)));promotion.setActive(true);promotion.setFundedBy("APP");em.persist(promotion);em.flush();
  put(user,promotion.getId(),source);
 }
 private void put(long user,long promotion,String source){var entry=new PromotionWallet();entry.userId=user;entry.promotionId=promotion;entry.source=source;entry.grantedAt=clock.instant();em.persist(entry);em.flush();}
 @Transactional(isolation=Isolation.READ_COMMITTED) public void save(UtetraPrincipal viewer,long id){
  account.require(viewer,true);db.user(viewer.userId(),true);
  var p=db.one("select p.* from promotions p where p.id=? and "+VISIBLE+" for update",id,viewer.userId());
  if(!flag(p,"is_active")||!clock.instant().isBefore(instant(p,"ends_at")))throw IdentityException.invalid("Ưu đãi không còn khả dụng.");
  if(db.count("select count(*) from promotion_wallet where user_id=? and promotion_id=?",viewer.userId(),id)==0)put(viewer.userId(),id,"SAVED:"+id);
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> list(UtetraPrincipal viewer){
  account.require(viewer,true);var result=db.list("select p.*,s.name store_name,w.source,w.granted_at,(select count(*) from coupon_redemptions r where r.promotion_id=p.id and r.user_id=? and r.status in ('RESERVED','USED')) own_used,(select count(*) from coupon_redemptions r where r.promotion_id=p.id and r.status in ('RESERVED','USED')) total_used from promotions p left join promotion_wallet w on w.promotion_id=p.id and w.user_id=? left join stores s on s.id=p.store_id where "+VISIBLE+" and (w.id is not null or (p.is_active=true and p.ends_at>?)) order by w.granted_at desc,p.ends_at,p.id",viewer.userId(),viewer.userId(),viewer.userId(),clock.instant());
  for(var p:result){String state=number(p,"own_used")>=number(p,"per_user_limit")?"USED":!clock.instant().isBefore(instant(p,"ends_at"))?"EXPIRED":!flag(p,"is_active")||number(p,"total_used")>=number(p,"usage_limit")?"UNAVAILABLE":clock.instant().isBefore(instant(p,"starts_at"))?"UPCOMING":"AVAILABLE";p.put("state",state);p.put("owned",p.get("source")!=null);p.put("reward",p.get("source")!=null&&!p.get("source").toString().startsWith("SAVED:"));}
  return result;
 }
}
