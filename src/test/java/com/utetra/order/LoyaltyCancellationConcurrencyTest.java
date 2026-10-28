package com.utetra.order;

import static org.assertj.core.api.Assertions.*;
import static com.utetra.order.OrderCommerceIntegrationTest.*;
import com.utetra.identity.IdentityRepository;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.repository.CommerceReferenceRepository;
import com.utetra.order.service.OrderProblem;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Gates real SELECT FOR UPDATE calls; fixtures and mutations are confined to H2. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({OrderCommerceIntegrationTest.TimeConfiguration.class,LoyaltyCancellationConcurrencyTest.Gates.class})
class LoyaltyCancellationConcurrencyTest {
    @Autowired AutowireCapableBeanFactory beans;
    @Autowired GatedIdentity identity;
    @Autowired GatedReferences refs;
    OrderCommerceIntegrationTest f;
    @BeforeEach void fixture(){f=new OrderCommerceIntegrationTest();beans.autowireBean(f);f.fixture();}
    static void await(CountDownLatch latch){
        try{if(!latch.await(8,TimeUnit.SECONDS))throw new IllegalStateException("Lock gate timed out");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
    static class GatedIdentity extends IdentityRepository {
        private final ThreadLocal<Runnable> before=new ThreadLocal<>();
        GatedIdentity(EntityManager em){super(em);}
        public void beforeUser(Runnable callback){if(callback==null)before.remove();else before.set(callback);}
        @Override public Map<String,Object> one(String sql,Object... args){
            if(sql.equals("select id from users where id=? for update")&&before.get()!=null)before.get().run();
            return super.one(sql,args);
        }
    }
    static class GatedReferences extends CommerceReferenceRepository {
        private final ThreadLocal<Runnable> before=new ThreadLocal<>(),after=new ThreadLocal<>();
        GatedReferences(JdbcTemplate jdbc){super(jdbc);}
        public void gates(Runnable b,Runnable a){if(b==null)before.remove();else before.set(b);if(a==null)after.remove();else after.set(a);}
        @Override public Map<String,Object> one(String sql,Object... args){
            boolean user=sql.equals("select id from users where id=? for update");
            if(user&&before.get()!=null)before.get().run();
            var result=super.one(sql,args);
            if(user&&after.get()!=null)after.get().run();
            return result;
        }
    }
    @TestConfiguration static class Gates {
        @Bean @Primary GatedIdentity gatedIdentity(EntityManager em){return new GatedIdentity(em);}
        @Bean @Primary GatedReferences gatedReferences(JdbcTemplate jdbc){return new GatedReferences(jdbc);}
    }
    @Test void operatorCanRefundPointsWhenBuyerIsInactive(){
        long order=f.id(f.create(1));
        long version=f.version(order);
        f.jdbc.update("update users set is_active=false where id=1");
        f.workflow.cancel(ADMIN,order,version,"Operator cancellation for inactive buyer");
        assertThat(f.jdbc.queryForObject("select balance from loyalty_accounts where user_id=1",Integer.class)).isEqualTo(100);
        assertThat(f.jdbc.queryForList("select points_delta from loyalty_transactions where order_id=? and type='REFUND'",Integer.class,order)).containsExactly(1);
    }
    @RepeatedTest(3) void groupAndDirectCancelRefundPointsOnceWithoutLockCycle() throws Exception {
        f.jdbc.update("update product_variants set stock_quantity=10 where id=1");
        String token=f.groupWithThree("HOST");f.groups.lock(BUYER,token,f.groupVersion(token));
        f.carts.selectBranch(BUYER,1,((Number)f.carts.view(BUYER).get("version")).longValue());
        String key=UUID.randomUUID().toString();var draft=new Checkout(key,1,1,1,null,null,1,"","","COD");
        String hash=f.checkout.quote(BUYER,draft).get("hash").toString();
        long order=f.id(f.checkout.checkout(BUYER,new Checkout(key,1,1,1,null,null,1,hash,"","COD")));
        long orderVersion=f.version(order),groupVersion=f.groupVersion(token);
        var directAtUser=new CountDownLatch(1);var groupedHasUser=new CountDownLatch(1);
        Runnable directGate=()->{directAtUser.countDown();await(groupedHasUser);};
        try(var pool=Executors.newFixedThreadPool(2)){
            Future<Throwable> direct=pool.submit(()->{
                identity.beforeUser(directGate);refs.gates(directGate,null);
                try{f.workflow.cancel(ADMIN,order,orderVersion,"Concurrent direct cancellation");return null;}
                catch(Throwable e){return e;}finally{identity.beforeUser(null);refs.gates(null,null);}
            });
            await(directAtUser);
            Future<Throwable> grouped=pool.submit(()->{
                refs.gates(null,groupedHasUser::countDown);
                try{f.groups.cancel(BUYER,token,groupVersion);return null;}
                catch(Throwable e){return e;}finally{refs.gates(null,null);}
            });
            Throwable a=direct.get(20,TimeUnit.SECONDS),b=grouped.get(20,TimeUnit.SECONDS);
            assertThat(b).as("Group cancellation must not be a deadlock victim").isNull();
            if(a!=null){
                assertThat(a).isInstanceOf(OrderProblem.class); // stale version/state conflict is allowed
                assertThat(((OrderProblem)a).status().value()).isEqualTo(409);
            }
            assertThat(f.jdbc.queryForObject("select status from orders where id=?",String.class,order)).isEqualTo("CANCELLED");
            assertThat(f.groups.view(BUYER,token)).containsEntry("status","CANCELLED");
            assertThat(f.jdbc.queryForObject("select balance from loyalty_accounts where user_id=1",Integer.class)).isEqualTo(100);
            assertThat(f.jdbc.queryForList("select points_delta from loyalty_transactions where order_id=? and type='REFUND'",Integer.class,order)).containsExactly(1);
            assertThat(f.jdbc.queryForList("select points_delta from loyalty_transactions where order_id=? and type='REDEEM'",Integer.class,order)).containsExactly(-1);
            f.groups.cancel(BUYER,token,f.groupVersion(token));
            assertThat(f.jdbc.queryForObject("select count(*) from loyalty_transactions where order_id=?",Integer.class,order)).isEqualTo(2);
            assertThat(f.jdbc.queryForObject("select stock_quantity from product_variants where id=1",Integer.class)).isEqualTo(10);
            assertThat(f.jdbc.queryForObject("select count(*) from order_items where order_id=? and stock_reserved=true",Integer.class,order)).isZero();
        }finally{groupedHasUser.countDown();}
    }
}
