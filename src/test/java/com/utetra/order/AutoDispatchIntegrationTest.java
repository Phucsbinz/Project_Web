package com.utetra.order;
import static org.assertj.core.api.Assertions.*;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"features.shipper.enabled=true","order.auto-dispatch.enabled=true"})
@ActiveProfiles("test") @Import(OrderCommerceIntegrationTest.TimeConfiguration.class)
class AutoDispatchIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;@Autowired OrderCommerceIntegrationTest.MutableClock clock;
 @Autowired CartService carts;@Autowired CheckoutService checkout;@Autowired OrderWorkflowService workflow;
 OrderCommerceIntegrationTest f;
 @BeforeEach void seed(){f=new OrderCommerceIntegrationTest();f.jdbc=jdbc;f.passwords=passwords;f.time=clock;f.carts=carts;f.checkout=checkout;f.workflow=workflow;f.fixture();for(long id:List.of(7L,8L)){jdbc.update("insert into users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version) select ?,5,?,?,password_hash,?,true,email_verified_at,created_at,updated_at,0 from users where id=5",id,"shipper"+id,"s"+id+"@test.invalid","Shipper "+id);jdbc.update("insert into shipper_store_scopes(shipper_id,store_id,is_active,updated_at,version) values(?,1,true,?,0)",id,clock.instant());}}
 long order(){return f.id(f.create(0));}
 void confirm(long id,Actor actor){workflow.transition(actor,id,new Transition("CONFIRMED",f.version(id),"",false));}
 long assigned(long id){return jdbc.queryForObject("select shipper_id from delivery_assignments where order_id=? and status='ACTIVE'",Long.class,id);}
 @Test void distributesToLeastLoadedAndNotifiesShipper(){var ids=new ArrayList<Long>();for(int i=0;i<4;i++){long id=order();confirm(id,OrderCommerceIntegrationTest.VENDOR);ids.add(id);}assertThat(ids.stream().map(this::assigned).toList()).containsExactly(5L,7L,8L,5L);assertThat(jdbc.queryForObject("select count(*) from notifications where user_id=5 and type='ASSIGNED'",Long.class)).isEqualTo(2);}
 @Test void noEligibleShipperKeepsConfirmedAndWaits(){jdbc.update("update shipper_store_scopes set is_active=false");long id=order();confirm(id,OrderCommerceIntegrationTest.MANAGER);assertThat(workflow.read(OrderCommerceIntegrationTest.BUYER,id)).containsEntry("status","CONFIRMED").containsEntry("dispatchStatus","WAITING");assertThat(jdbc.queryForObject("select count(*) from delivery_assignments where order_id=?",Long.class,id)).isZero();}
 @Test void excludesInactiveUnverifiedAndDifferentBranch(){jdbc.update("update users set is_active=false where id=5");jdbc.update("update users set email_verified_at=null where id=7");jdbc.update("update shipper_store_scopes set store_id=2 where shipper_id=8");long id=order();confirm(id,OrderCommerceIntegrationTest.ADMIN);assertThat(jdbc.queryForObject("select count(*) from delivery_assignments",Long.class)).isZero();}
 @Test void concurrentConfirmsStayBalancedAndNeverDuplicate()throws Exception{long a=order(),b=order();var pool=Executors.newFixedThreadPool(2);try{var x=pool.submit(()->confirm(a,OrderCommerceIntegrationTest.ADMIN));var y=pool.submit(()->confirm(b,OrderCommerceIntegrationTest.VENDOR));x.get(15,TimeUnit.SECONDS);y.get(15,TimeUnit.SECONDS);assertThat(assigned(a)).isNotEqualTo(assigned(b));assertThat(jdbc.queryForObject("select count(*) from delivery_assignments",Long.class)).isEqualTo(2);assertThatThrownBy(()->confirm(a,OrderCommerceIntegrationTest.ADMIN)).isInstanceOf(OrderProblem.class);assertThat(jdbc.queryForObject("select count(*) from delivery_assignments",Long.class)).isEqualTo(2);}finally{pool.shutdownNow();}}
 @Test void waitingOrderCanRetryWithoutChangingAnExistingAssignment(){jdbc.update("update shipper_store_scopes set is_active=false");long id=order();confirm(id,OrderCommerceIntegrationTest.VENDOR);jdbc.update("update shipper_store_scopes set is_active=true where shipper_id=7");workflow.retryDispatch(OrderCommerceIntegrationTest.VENDOR,id,f.version(id));assertThat(assigned(id)).isEqualTo(7);long version=f.version(id);workflow.retryDispatch(OrderCommerceIntegrationTest.VENDOR,id,version);assertThat(f.version(id)).isEqualTo(version);assertThat(jdbc.queryForObject("select count(*) from delivery_assignment_changes c join delivery_assignments a on a.id=c.assignment_id where a.order_id=?",Long.class,id)).isEqualTo(1);}

 @Test void operatorCannotAdvanceDeliveryButAssignedShipperCan(){long id=order();confirm(id,OrderCommerceIntegrationTest.ADMIN);for(var actor:List.of(OrderCommerceIntegrationTest.ADMIN,OrderCommerceIntegrationTest.MANAGER,OrderCommerceIntegrationTest.VENDOR))assertThatThrownBy(()->workflow.transition(actor,id,new Transition("PICKED_UP",f.version(id),"",false))).isInstanceOf(OrderProblem.class);long shipper=assigned(id);workflow.transition(new Actor(shipper,"ROLE_SHIPPER"),id,new Transition("PICKED_UP",f.version(id),"",false));assertThat(workflow.read(OrderCommerceIntegrationTest.BUYER,id)).containsEntry("status","PICKED_UP");}

}
