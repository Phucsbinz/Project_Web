package com.utetra.order;
import static org.assertj.core.api.Assertions.*;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import com.utetra.order.service.*;
import java.time.*; import java.util.*;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"features.shipper.enabled=true","spring.datasource.url=jdbc:h2:mem:utetra_shipping_scope;MODE=MySQL;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("test")
class ShippingScopeIntegrationTest {
 @org.springframework.boot.test.context.TestConfiguration
 static class WebRoot {
  @org.springframework.context.annotation.Bean
  org.springframework.boot.web.server.WebServerFactoryCustomizer<org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory> root(){return factory->{var root=java.nio.file.Path.of("").toAbsolutePath();while(root!=null&&!java.nio.file.Files.isDirectory(root.resolve("src/main/webapp")))root=root.getParent();if(root==null)throw new IllegalStateException("web root missing");factory.setDocumentRoot(root.resolve("src/main/webapp").toFile());};}
 }
 @Autowired org.springframework.boot.test.web.client.TestRestTemplate http;
 @Autowired com.utetra.security.JwtService jwt;
 @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder passwords;
 @Autowired OrderWorkflowService workflow; @Autowired OperationsService operations;
 @Autowired ShipperScopeService scopes;
 @Autowired OrderDataRepository data; @Autowired org.springframework.transaction.PlatformTransactionManager tx;
 static final Actor ADMIN=new Actor(4,"ROLE_ADMIN"), MANAGER=new Actor(3,"ROLE_MANAGER"), SHIPPER=new Actor(5,"ROLE_SHIPPER");
 @BeforeEach void fixture(){
 jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
 for(var t:jdbc.queryForList("select table_name from information_schema.tables where table_schema='PUBLIC'",String.class)) jdbc.execute("delete from "+t);
 jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    for (int i = 1; i <= 5; i++)
      jdbc.update(
          "insert into roles(id,name) values(?,?)",
          i,
          List.of("ROLE_USER", "ROLE_VENDOR", "ROLE_MANAGER", "ROLE_ADMIN", "ROLE_SHIPPER")
              .get(i - 1));
    String password = passwords.encode("Only-test-user-password");
    for (int i = 1; i <= 6; i++) {
      jdbc.update(
          "insert into"
              + " users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version)"
              + " values(?,?,?,?,?,?,true,?,?,?,0)",
          i,
          i == 6 ? 1 : i,
          "user" + i,
          "u" + i + "@test.invalid",
          password,
          "User " + i,
          Instant.parse("2026-10-14T03:00:00Z"),
          Instant.parse("2026-10-14T03:00:00Z"),
          Instant.parse("2026-10-14T03:00:00Z"));
      jdbc.update(
          "insert into auth_sessions(id,user_id,refresh_token_hash,created_at,expires_at,version)"
              + " values(?,?,?,?,?,0)",
          i,
          i,
          "hash" + i,
          Instant.parse("2026-10-14T03:00:00Z"),
          Instant.parse("2026-10-14T03:00:00Z").plus(Duration.ofDays(7)));
    }
    jdbc.update(
        "insert into"
            + " commission_policies(id,name,rate_percent,is_active,created_at,updated_at,version)"
            + " values(1,'Default',10,true,?,?,0)",
        Instant.parse("2026-10-14T03:00:00Z"),
        Instant.parse("2026-10-14T03:00:00Z"));
    for (int id = 1; id <= 2; id++)
      jdbc.update(
          "insert into"
              + " stores(id,owner_user_id,commission_policy_id,name,slug,address,phone,open_time,close_time,status,accepting_orders,created_at,updated_at,version)"
              + " values(?,2,1,?,?,"
              + " 'Address','0900000000','00:00:00','23:59:59','ACTIVE',true,?,?,0)",
          id,
          "Shop " + id,
          "shop-" + id,
          Instant.parse("2026-10-14T03:00:00Z"),
          Instant.parse("2026-10-14T03:00:00Z"));
    jdbc.update(
        "insert into"
            + " store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at)"
            + " values(3,1,'MANAGER',true,4,?,?)",
        Instant.parse("2026-10-14T03:00:00Z"),
        Instant.parse("2026-10-14T03:00:00Z"));

 }
 long order(String status){return new TransactionTemplate(tx).execute(s->{
 Order o=new Order();o.orderCode="T-"+UUID.randomUUID();o.userId=1L;o.storeId=1L;o.deliveryAreaId=1L;o.carrierId=1L;o.commissionPolicyId=1L;
 o.checkoutKey=UUID.randomUUID().toString();o.requestHash="hash";o.recipientName="Private";o.recipientPhone="0900000000";o.deliveryAddress="Private address";
 o.storeName="Shop";o.deliveryAreaName="Area";o.carrierName="Carrier";o.paymentMethod="COD";o.paymentStatus="UNPAID";o.status=status;o.createdAt=Instant.now();o.updatedAt=o.createdAt;data.save(o);data.flush();return o.id;});}
 @Test void unscopedShipperIsNotAnAvailableDeliveryCandidate(){assertThat(workflow.availableShippers(MANAGER,order("NEW"))).isEmpty();}
 @Test void normalAssignmentCannotStartAfterPickup(){assertThatThrownBy(()->workflow.assign(MANAGER,order("PICKED_UP"),new Assignment(5,0))).isInstanceOf(OrderProblem.class);}
 @Test void managerCannotReadOwnerSettlementFigures(){assertThatThrownBy(()->operations.revenue(MANAGER,1L,LocalDate.now(),LocalDate.now())).isInstanceOf(OrderProblem.class);}
 @Test void adminScopeIsDistinctFromOrderAssignmentAndRevokeRemovesPiiAccess(){
   long id=order("CONFIRMED");
   assertThatThrownBy(()->workflow.assign(MANAGER,id,new Assignment(5,0))).isInstanceOf(OrderProblem.class);
   scopes.set(ADMIN,5,1,true,0,"Service region approved");
   assertThat(workflow.list(SHIPPER,"shipper",null,0,20)).isEmpty();
   workflow.assign(MANAGER,id,new Assignment(5,0));
   assertThat(workflow.read(SHIPPER,id)).containsEntry("recipientPhone","0900000000");
   scopes.set(ADMIN,5,1,false,0,"Region removed");
   assertThatThrownBy(()->workflow.read(SHIPPER,id)).isInstanceOf(OrderProblem.class);
   assertThat(workflow.list(SHIPPER,"shipper",null,0,20)).isEmpty();
   assertThat(jdbc.queryForObject("select count(*) from delivery_assignments",Long.class)).isEqualTo(1);
   assertThat(jdbc.queryForObject("select count(*) from shipper_scope_events",Long.class)).isEqualTo(2);
 }
 @Test void reassignmentKeepsOldAndNewShipperAuditButRemovesPreviousAccess(){
   jdbc.update("update users set role_id=5 where id=6");
   scopes.set(ADMIN,5,1,true,0,"Region");scopes.set(ADMIN,6,1,true,0,"Region");long id=order("CONFIRMED");
   workflow.assign(MANAGER,id,new Assignment(5,0));
   long version=((Number)workflow.read(MANAGER,id).get("version")).longValue();
   assertThatThrownBy(()->workflow.assign(MANAGER,id,new Assignment(6,version))).isInstanceOf(OrderProblem.class);
   workflow.assign(MANAGER,id,new Assignment(6,version,"Shift changed"));
   assertThatThrownBy(()->workflow.read(SHIPPER,id)).isInstanceOf(OrderProblem.class);
   assertThat(jdbc.queryForObject("select count(*) from delivery_assignment_changes",Long.class)).isEqualTo(2);
   var change=jdbc.queryForMap("select * from delivery_assignment_changes where previous_shipper_id=5");
   assertThat(((Number)change.get("shipper_id")).longValue()).isEqualTo(6);assertThat(change.get("reason")).isEqualTo("Shift changed");
 }
 @Test void completedDeliveriesExposeOnlyAggregateHistory(){
   scopes.set(ADMIN,5,1,true,0,"Region");long id=order("CONFIRMED");workflow.assign(MANAGER,id,new Assignment(5,0));
   jdbc.update("update delivery_assignments set status='COMPLETED' where order_id=?",id);
   assertThatThrownBy(()->workflow.read(SHIPPER,id)).isInstanceOf(OrderProblem.class);
   assertThat(workflow.list(SHIPPER,"shipper",null,0,20)).singleElement().satisfies(row->assertThat(row).containsEntry("canRead",false).doesNotContainKeys("recipientPhone","deliveryAddress","total","note"));
 }
 @Test void managerOperationalReportHasNoSettlementFields(){
   order("NEW");var report=operations.operationalReport(MANAGER,1L,LocalDate.now().minusDays(1),LocalDate.now().plusDays(1));
   assertThat(report).containsEntry("orders",1L).containsKeys("codDue","codCollected").doesNotContainKeys("goods","commission","storeNet");
 }

 @Test void scopeCannotBeGrantedByManagerOrToInactiveShipper(){
   assertThatThrownBy(()->scopes.set(MANAGER,5,1,true,0,"Region")).isInstanceOf(OrderProblem.class);
   jdbc.update("update users set is_active=false where id=5");
   assertThatThrownBy(()->scopes.set(ADMIN,5,1,true,0,"Region")).isInstanceOf(OrderProblem.class);
   assertThat(jdbc.queryForObject("select count(*) from shipper_store_scopes",Long.class)).isZero();
 }
 @Test void revokeAndAssignmentSerializeOnShipperAccount() throws Exception {
   scopes.set(ADMIN,5,1,true,0,"Region");long id=order("CONFIRMED");
   var revoked=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
   var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
   try {
    var revoke=pool.submit(()->new TransactionTemplate(tx).execute(s->{scopes.set(ADMIN,5,1,false,0,"Region revoked");revoked.countDown();try{if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return null;}));
    assertThat(revoked.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    var attempted=new java.util.concurrent.CountDownLatch(1);
    var assign=pool.submit(()->{attempted.countDown();try{workflow.assign(MANAGER,id,new Assignment(5,0));return "assigned";}catch(OrderProblem expected){return "denied";}});
    assertThat(attempted.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    assertThatThrownBy(()->assign.get(200,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
    release.countDown();revoke.get(10,java.util.concurrent.TimeUnit.SECONDS);
    assertThat(assign.get(10,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("denied");
    assertThat(jdbc.queryForObject("select count(*) from delivery_assignments",Long.class)).isZero();
   } finally {release.countDown();pool.shutdownNow();assertThat(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();}
 }

 @Test void scopeAndOperationalPagesRenderWithOneSharedTomcatLayout(){
   jdbc.update("update auth_sessions set expires_at=?",Instant.now().plus(Duration.ofDays(1)));
   scopes.set(ADMIN,5,1,true,0,"Region");long id=order("CONFIRMED");workflow.assign(MANAGER,id,new Assignment(5,0));
   for(var pair:List.of(Map.entry(ADMIN,"/admin/shipper-scopes"),Map.entry(MANAGER,"/manager/reports"),Map.entry(SHIPPER,"/shipper/orders/"+id))){
    var actor=pair.getKey();var headers=new org.springframework.http.HttpHeaders();headers.setBearerAuth(jwt.issue(actor.userId(),actor.userId(),actor.role()));
    var response=http.exchange(pair.getValue(),org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(headers),String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("workspace-sidebar").doesNotContain("JasperException","Exception Report","const groupOrder=;");
    assertThat(response.getBody().split("id=\"workspace-navigation\"",-1)).hasSize(2);
   }
   jdbc.update("update delivery_assignments set status='COMPLETED' where order_id=?",id);
   var headers=new org.springframework.http.HttpHeaders();headers.setBearerAuth(jwt.issue(5L,5L,"ROLE_SHIPPER"));
   var history=http.exchange("/shipper/orders",org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(headers),String.class);
   assertThat(history.getStatusCode().value()).isEqualTo(200);
   assertThat(history.getBody()).doesNotContain("Private address","0900000000","Xem đơn / cập nhật giao hàng");
 }

 @Test void vendorCannotDispatchDeliveryAndShipperSeesOnlyTripFields(){
   scopes.set(ADMIN,5,1,true,0,"Region");long id=order("CONFIRMED");
   assertThatThrownBy(()->workflow.assign(new Actor(2,"ROLE_VENDOR"),id,new Assignment(5,0))).isInstanceOf(OrderProblem.class);
   workflow.assign(MANAGER,id,new Assignment(5,0));
   assertThat(workflow.read(SHIPPER,id)).containsKeys("recipientPhone","pickupAddress","items","version").doesNotContainKeys("returns","pointsUsed","appliedVouchers","groupToken","qrCollected","myShare","shippingGross","subtotal");
 }

 @Test void selectedManagerStoreFiltersOrdersAndRejectsUnassignedBranch(){
   order("NEW");assertThat(workflow.list(MANAGER,"operator",null,0,20,1L)).hasSize(1);
   assertThatThrownBy(()->workflow.list(MANAGER,"operator",null,0,20,2L)).isInstanceOf(OrderProblem.class);
 }

 @Test void crossOwnerCancellationLocksBuyerAndOperatorInStableOrder() throws Exception {
   jdbc.update("update users set role_id=2 where id=6");jdbc.update("update stores set owner_user_id=6 where id=2");
   long id=order("NEW");jdbc.update("update orders set user_id=2,store_id=2 where id=?",id);
   var held=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
   var pool=java.util.concurrent.Executors.newFixedThreadPool(3);
   try {
    var holder=pool.submit(()->new TransactionTemplate(tx).execute(s->{jdbc.queryForObject("select id from users where id=2 for update",Long.class);held.countDown();try{if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("release timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return null;}));
    assertThat(held.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    var cancel=pool.submit(()->workflow.cancel(new Actor(6,"ROLE_VENDOR"),id,0,"Customer cancellation"));
    long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    while(jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Long.class)==0&&System.nanoTime()<deadline)Thread.sleep(10);
    assertThat(jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Long.class)).isPositive();
    // The waiting operation must not already hold higher user 6 while it waits for lower user 2.
    var probe=pool.submit(()->new TransactionTemplate(tx).execute(s->jdbc.queryForObject("select id from users where id=6 for update",Long.class)));
    assertThat(probe.get(2,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(6);
    release.countDown();holder.get(5,java.util.concurrent.TimeUnit.SECONDS);assertThat(cancel.get(5,java.util.concurrent.TimeUnit.SECONDS)).containsEntry("status","CANCELLED");
   }finally{release.countDown();pool.shutdownNow();assertThat(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();}
 }

}
