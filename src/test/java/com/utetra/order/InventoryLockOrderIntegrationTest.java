package com.utetra.order;

import static org.assertj.core.api.Assertions.*;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.repository.CommerceReferenceRepository;
import com.utetra.order.service.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Observes real, bound point SQL against H2; does not claim MySQL/TiDB deadlock coverage. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
@Import({OrderCommerceIntegrationTest.TimeConfiguration.class,InventoryLockOrderIntegrationTest.TraceConfiguration.class})
class InventoryLockOrderIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired OrderCommerceIntegrationTest.MutableClock clock;
    @Autowired CartService carts;
    @Autowired CheckoutService checkout;
    @Autowired OrderWorkflowService workflow;
    @Autowired RecordingReferences refs;
    OrderCommerceIntegrationTest fixture;
    static final Actor BUYER=OrderCommerceIntegrationTest.BUYER, OTHER=OrderCommerceIntegrationTest.BUYER_B;
    record Lock(String kind,long variant) {}
    static class RecordingReferences extends CommerceReferenceRepository {
        final ThreadLocal<List<Lock>> executed=new ThreadLocal<>();
        final ThreadLocal<Consumer<Lock>> before=new ThreadLocal<>(),after=new ThreadLocal<>();
        RecordingReferences(JdbcTemplate jdbc){super(jdbc);}
        private Lock readLock(String sql,Object[] args){
            if(sql.contains("product_variants")&&sql.endsWith("for update"))return new Lock(sql.contains("where v.id=?")?"SALE":"STOCK",((Number)args[0]).longValue());
            return null;
        }
        private void before(Lock lock){if(lock!=null&&before.get()!=null)before.get().accept(lock);}
        private void completed(Lock lock){if(lock==null)return;if(executed.get()!=null)executed.get().add(lock);if(after.get()!=null)after.get().accept(lock);}
        @Override public Map<String,Object> one(String sql,Object... args){Lock lock=readLock(sql,args);before(lock);var row=super.one(sql,args);completed(lock);return row;}
        @Override public int write(String sql,Object... args){
            Lock lock=sql.startsWith("update product_variants set stock_quantity=stock_quantity+")?new Lock("RELEASE",((Number)args[args.length-1]).longValue()):null;
            before(lock);int changed=super.write(sql,args);completed(lock);return changed;
        }
        List<Long> variants(String kind){return executed.get().stream().filter(l->l.kind().equals(kind)).map(Lock::variant).distinct().toList();}
        public void begin(){executed.set(new ArrayList<>());}
        public void resetEvents(){executed.get().clear();}
        public void onBefore(Consumer<Lock> observer){before.set(observer);}
        public void onAfter(Consumer<Lock> observer){after.set(observer);}
        public void clear(){executed.remove();before.remove();after.remove();}
    }
    @TestConfiguration static class TraceConfiguration {
        @Bean @Primary RecordingReferences recordingReferences(JdbcTemplate jdbc){return new RecordingReferences(jdbc);}
    }
    @BeforeEach void fixture(){
        fixture=new OrderCommerceIntegrationTest();fixture.jdbc=jdbc;fixture.passwords=passwords;fixture.time=clock;fixture.fixture();
        jdbc.update("insert into products(id,store_id,category_id,name,slug,base_price,is_active,is_available,is_featured,created_at,updated_at,version) values(3,1,1,'Second tea','second-tea',30000,true,true,false,?,?,0)",clock.instant(),clock.instant());
        jdbc.update("insert into product_variants(id,product_id,size_name,extra_price,is_active,stock_quantity,created_at,updated_at,version) values(3,3,'M',5000,true,10,?,?,0)",clock.instant(),clock.instant());
        jdbc.update("update product_variants set stock_quantity=10 where id=1");
        refs.begin();
    }
    @AfterEach void cleanup(){refs.clear();}
    Item item(long variant){return new Item(variant,50,50,List.of(),1,"");}
    Checkout draft(Actor actor,String key){return new Checkout(key,actor.userId()==1?1:2,1,1,null,null,0,"","","COD");}
    Checkout quoted(Actor actor){var d=draft(actor,UUID.randomUUID().toString());return new Checkout(d.key(),d.addressId(),1,1,null,null,0,checkout.quote(actor,d).get("hash").toString(),"","COD");}
    long place(Actor actor){return ((Number)checkout.checkout(actor,quoted(actor)).get("id")).longValue();}
    long version(long id){return jdbc.queryForObject("select version from orders where id=?",Long.class,id);}
    void add(Actor actor,long first,long second){carts.add(actor,item(first));carts.add(actor,item(second));}

    @Test void checkoutLocksStableVariantIdsWithoutReorderingOrderSnapshots(){
        add(BUYER,3,1);refs.resetEvents();long id=place(BUYER);
        assertThat(refs.variants("SALE")).containsExactly(1L,3L);
        assertThat(jdbc.queryForList("select variant_id from order_items where order_id=? order by id",Long.class,id)).containsExactly(3L,1L);
        assertThat(jdbc.queryForList("select stock_quantity from product_variants where id in (1,3) order by id",Integer.class)).containsExactly(9,9);
    }
    @Test void cancellationReleasesInStableVariantOrderExactlyOnce(){
        add(BUYER,3,1);long id=place(BUYER);refs.resetEvents();
        workflow.cancel(BUYER,id,version(id),"Cancel before preparation");
        assertThat(refs.variants("RELEASE")).containsExactly(1L,3L);
        assertThatThrownBy(()->workflow.cancel(BUYER,id,version(id),"Retry")).isInstanceOf(OrderProblem.class);
        assertThat(jdbc.queryForList("select stock_quantity from product_variants where id in (1,3) order by id",Integer.class)).containsExactly(10,10);
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id=? and stock_reserved=true",Integer.class,id)).isZero();
    }
    @Test void guestMergeLocksVariantsInStableOrderWithoutChangingDisplayOrder(){
        carts.merge(BUYER,List.of(item(3),item(1)),UUID.randomUUID().toString());
        assertThat(refs.variants("SALE")).containsExactly(1L,3L);
        assertThat(jdbc.queryForList("select variant_id from cart_items order by id",Long.class)).containsExactly(3L,1L);
        assertThat(jdbc.queryForList("select stock_quantity from product_variants where id in (1,3) order by id",Integer.class)).containsExactly(10,10);
    }
    @RepeatedTest(3) void coordinatedCancelAndOppositeCartCheckoutKeepFinalStockAndIdempotency() throws Exception {
        add(BUYER,3,1);long old=place(BUYER);add(OTHER,1,3);Checkout request=quoted(OTHER);
        var releaseHeld=new CountDownLatch(1);var checkoutAttempted=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var cancel=pool.submit(()->{
                refs.onAfter(lock->{if(lock.kind().equals("RELEASE")&&lock.variant()==1){releaseHeld.countDown();await(checkoutAttempted);}});
                try{return workflow.cancel(BUYER,old,version(old),"Coordinated cancellation");}finally{refs.clear();}
            });
            assertThat(releaseHeld.await(5,TimeUnit.SECONDS)).isTrue();
            var purchase=pool.submit(()->{
                refs.onBefore(lock->{if(lock.kind().equals("SALE")&&lock.variant()==1)checkoutAttempted.countDown();});
                try{return checkout.checkout(OTHER,request);}finally{refs.clear();}
            });
            assertThat(cancel.get(10,TimeUnit.SECONDS)).containsEntry("status","CANCELLED");
            long fresh=((Number)purchase.get(10,TimeUnit.SECONDS).get("id")).longValue();
            assertThat(((Number)checkout.checkout(OTHER,request).get("id")).longValue()).isEqualTo(fresh);
            assertThat(jdbc.queryForList("select stock_quantity from product_variants where id in (1,3) order by id",Integer.class)).containsExactly(9,9);
            assertThat(jdbc.queryForObject("select count(*) from orders where user_id=6",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from order_items where order_id=? and stock_reserved=true",Integer.class,old)).isZero();
        }finally{checkoutAttempted.countDown();}
    }
    static void await(CountDownLatch latch){try{if(!latch.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Concurrency gate timed out");}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}}
}
