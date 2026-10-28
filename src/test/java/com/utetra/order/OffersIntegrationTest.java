package com.utetra.order;

import com.utetra.identity.*;
import com.utetra.order.service.*;
import com.utetra.security.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(OrderCommerceIntegrationTest.TimeConfiguration.class)
class OffersIntegrationTest {
 @Autowired OfferWalletService wallet;
 @Autowired com.utetra.service.PromotionService catalogPromotions;
 @Autowired JdbcTemplate jdbc;
 @Autowired PasswordEncoder passwords;
 @Autowired JwtService jwt;
 @Autowired OrderPromotionService promotions;
 @Autowired CartService carts;
 @Autowired GroupOrderService groups;
 @Autowired CheckoutService checkout;
 @Autowired OrderWorkflowService workflow;
 @Autowired OrderCommerceIntegrationTest.MutableClock clock;
 private UtetraPrincipal buyer;
 @BeforeEach void seed(){var f=new OrderCommerceIntegrationTest();f.jdbc=jdbc;f.passwords=passwords;f.time=clock;f.fixture();buyer=jwt.verify(jwt.issue(1L,1L,"ROLE_USER"));}
 @Test void tiersIssueOnceAndPrivateRewardsCannotBeUsedByAnotherBuyer(){
  jdbc.update("update loyalty_accounts set lifetime_earned=500 where user_id=1");
  wallet.sync(buyer);wallet.sync(buyer);
  assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where user_id=1",Long.class)).isEqualTo(2);
  long id=jdbc.queryForObject("select promotion_id from promotion_wallet where user_id=1 and source='TIER_SILVER'",Long.class);
  assertThat(promotions.discount(id,1,1,"PRODUCT_PERCENT",new BigDecimal("200000"),new BigDecimal("200000"),Map.of(),false)).isEqualByComparingTo("20000");
  assertThatThrownBy(()->promotions.discount(id,6,1,"PRODUCT_PERCENT",new BigDecimal("200000"),new BigDecimal("200000"),Map.of(),false)).isInstanceOf(OrderProblem.class);
  assertThat(catalogPromotions.available(1L).toString()).doesNotContain("Quà hạng Bạc","Quà hạng Vàng");
  assertThatThrownBy(()->catalogPromotions.quote(id,1L,Map.of(1L,new BigDecimal("200000")),BigDecimal.ZERO)).isInstanceOf(RuntimeException.class);
  var other=jwt.verify(jwt.issue(6L,6L,"ROLE_USER"));
  assertThat(wallet.list(other).toString()).doesNotContain("Quà hạng Bạc","Quà hạng Vàng");
  assertThatThrownBy(()->wallet.save(other,id)).isInstanceOf(IdentityException.class);
  jdbc.update("update auth_sessions set expires_at=? where id=1",clock.instant().plus(Duration.ofDays(60)));
  clock.advance(Duration.ofDays(31));
  buyer=jwt.verify(jwt.issue(1L,1L,"ROLE_USER"));
  assertThat(wallet.list(buyer).toString()).contains("EXPIRED");
  assertThatThrownBy(()->promotions.discount(id,1,1,"PRODUCT_PERCENT",new BigDecimal("200000"),new BigDecimal("200000"),Map.of(),false)).isInstanceOf(OrderProblem.class);
 }
 @Test void publicVoucherSaveIsIdempotentAndSessionIsRechecked(){
  jdbc.update("insert into promotions(id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version) values(88,4,'GLOBAL','SHIPPING_PERCENT','SHIP88','Giao hàng',100,15000,0,100,1,?,?,true,'APP',?,?,0)",clock.instant(),clock.instant().plusSeconds(3600),clock.instant(),clock.instant());
  wallet.save(buyer,88);wallet.save(buyer,88);
  assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where promotion_id=88",Long.class)).isEqualTo(1);
  assertThat(wallet.list(buyer).toString()).contains("Giao hàng","SAVED");
  jdbc.update("update auth_sessions set revoked_at=? where id=1",clock.instant());
  assertThatThrownBy(()->wallet.list(buyer)).isInstanceOf(IdentityException.class);
 }
 @Test void deliveredPaidOrdersAwardShippingAtFiveOnlyOnceAndNotForNewOrders(){
  var f=new OrderCommerceIntegrationTest();f.jdbc=jdbc;f.time=clock;f.carts=carts;f.checkout=checkout;f.workflow=workflow;
  var ids=new ArrayList<Long>();for(int i=0;i<5;i++)ids.add(f.id(f.create(0)));
  wallet.sync(buyer);
  assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where source='ORDERS_5'",Long.class)).isZero();
  for(long id:ids)f.delivered(id);
  assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where source='ORDERS_5'",Long.class)).isEqualTo(1);
  wallet.sync(buyer);wallet.sync(buyer);
  assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where source='ORDERS_5'",Long.class)).isEqualTo(1);
  long shipping=jdbc.queryForObject("select promotion_id from promotion_wallet where source='ORDERS_5'",Long.class);
  assertThat(promotions.discount(shipping,1,1,"SHIPPING_PERCENT",new BigDecimal("35000"),new BigDecimal("20000"),Map.of(),false)).isEqualByComparingTo("15000");
 }
 @Test void concurrentSyncDoesNotDuplicateTierGift()throws Exception{
  var pool=java.util.concurrent.Executors.newFixedThreadPool(2);try{var a=pool.submit(()->wallet.sync(buyer));var b=pool.submit(()->wallet.sync(buyer));a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where user_id=1 and source='TIER_SILVER'",Long.class)).isEqualTo(1);}finally{pool.shutdownNow();}
 }

 @Test void zeroPointDeliveriesStillAwardFivePurchaseGift(){
  jdbc.update("update products set base_price=1000");jdbc.update("update product_variants set extra_price=0");jdbc.update("update toppings set price=0");
  var f=new OrderCommerceIntegrationTest();f.jdbc=jdbc;f.time=clock;f.carts=carts;f.checkout=checkout;f.workflow=workflow;
  for(int i=0;i<5;i++){long id=f.id(f.create(0));f.delivered(id);assertThat(jdbc.queryForObject("select points_earned from orders where id=?",Long.class,id)).isZero();}
  assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where user_id=1 and source='ORDERS_5'",Long.class)).isEqualTo(1);
 }
 @Test void everyBilledMemberReceivesCommittedGroupPurchaseMilestone(){
  var f=new OrderCommerceIntegrationTest();f.jdbc=jdbc;f.time=clock;f.carts=carts;f.checkout=checkout;f.workflow=workflow;f.groups=groups;
  for(int i=0;i<5;i++){String token=f.groupWithThree("HOST");groups.lock(OrderCommerceIntegrationTest.BUYER,token,f.groupVersion(token));carts.selectBranch(OrderCommerceIntegrationTest.BUYER,1,((Number)carts.view(OrderCommerceIntegrationTest.BUYER).get("version")).longValue());String key=UUID.randomUUID().toString();var draft=new com.utetra.order.dto.OrderRequests.Checkout(key,1,1,1,null,null,0,"","","COD");var quote=checkout.quote(OrderCommerceIntegrationTest.BUYER,draft);long id=f.id(checkout.checkout(OrderCommerceIntegrationTest.BUYER,new com.utetra.order.dto.OrderRequests.Checkout(key,1,1,1,null,null,0,quote.get("hash").toString(),"","COD")));f.delivered(id);}
  for(int user:List.of(1,2,6))assertThat(jdbc.queryForObject("select count(*) from promotion_wallet where user_id=? and source='ORDERS_5'",Long.class,user)).isEqualTo(1);
 }

}
