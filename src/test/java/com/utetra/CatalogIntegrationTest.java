package com.utetra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.dto.CatalogRequests.*;
import com.utetra.dto.CatalogViews.*;
import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CatalogIntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration
    static class TestClock {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        Clock catalogTestClock() { return Clock.fixed(Instant.parse("2026-10-15T07:00:00Z"),ZoneOffset.UTC); }
    }
    @Autowired Clock clock;
    @Autowired StoreService stores;
    @Autowired CatalogService catalog;
    @Autowired PromotionService promotions;
    @Autowired StoreAccessPolicy policy;
    @Autowired StoreRepository storeRepo;
    @Autowired ProductRepository productRepo;
    @Autowired ProductVariantRepository variantRepo;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired com.utetra.security.JwtService jwt;
    UtetraPrincipal admin=principal(101,"ADMIN"),vendor=principal(102,"VENDOR"),other=principal(103,"VENDOR"),manager=principal(104,"MANAGER"),buyer=principal(105,"USER");
    Long storeId,categoryId,toppingId;
    static UtetraPrincipal principal(long id,String role) { return new UtetraPrincipal(id,id,"ROLE_"+role,Instant.now().plusSeconds(3600)); }
    static BigDecimal money(long x) { return BigDecimal.valueOf(x); }
    @BeforeAll static void prerequisite(@Autowired JdbcTemplate jdbc) {
        jdbc.execute("create table if not exists commission_policies(id bigint primary key,name varchar(100),rate_percent decimal(5,2),is_active boolean,created_at timestamp,updated_at timestamp,version bigint)");
        // Minimal owner contract tables, only in the disposable H2 test database.
        jdbc.execute("create table if not exists orders(id bigint primary key,status varchar(30),payment_status varchar(20))");
        jdbc.execute("create table if not exists order_items(id bigint primary key,order_id bigint,variant_id bigint,quantity int)");
        jdbc.execute("create table if not exists reviews(id bigint primary key,product_id bigint,stars int,status varchar(20))");
        jdbc.execute("create table if not exists product_favorites(id bigint primary key,product_id bigint,user_id bigint)");
    }
    @BeforeEach void setup() {
        for(String role:List.of("ADMIN","VENDOR","MANAGER","USER")) jdbc.update("insert into roles(name) select ? where not exists (select 1 from roles where name=?)","ROLE_"+role,"ROLE_"+role);
        for(var p:List.of(admin,vendor,other,manager,buyer)) jdbc.update("insert into users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version) values(?,(select id from roles where name=?),?,?,?,'Test',true,current_timestamp,current_timestamp,current_timestamp,0)",p.userId(),p.role(),"catalog"+p.userId(),"catalog"+p.userId()+"@example.test","not-a-login-password");
        for(var p:List.of(admin,vendor,other,manager,buyer)) jdbc.update("insert into auth_sessions(id,user_id,refresh_token_hash,created_at,expires_at,version) values(?,?,?,?,?,0)",p.sessionId(),p.userId(),String.format("%064d",p.userId()),clock.instant(),clock.instant().plusSeconds(86400));
        admin=jwt.verify(jwt.issue(admin.userId(),admin.sessionId(),admin.role()));
        vendor=jwt.verify(jwt.issue(vendor.userId(),vendor.sessionId(),vendor.role()));
        other=jwt.verify(jwt.issue(other.userId(),other.sessionId(),other.role()));
        manager=jwt.verify(jwt.issue(manager.userId(),manager.sessionId(),manager.role()));
        buyer=jwt.verify(jwt.issue(buyer.userId(),buyer.sessionId(),buyer.role()));
        jdbc.update("merge into commission_policies(id,name,rate_percent,is_active,created_at,updated_at,version) key(id) values(1,'Test policy',5,true,current_timestamp,current_timestamp,0)");
        storeId=stores.register(buyer,shop("shop-test",0,false)).id();
        stores.review(admin,storeId,new Decision("APPROVED","Đủ điều kiện",0,1L)); em.flush(); em.clear();
        stores.update(admin,storeId,shop("shop-test",storeRepo.findById(storeId).orElseThrow().getVersion(),true));
        categoryId=catalog.saveCategory(admin,null,new CategoryInput("Trà sữa","tra-sua","",0,true)).id();
        toppingId=catalog.saveTopping(admin,null,new ToppingInput("Trân châu",money(5000),true)).id();
        catalog.availability(admin,storeId,toppingId,true);
    }
    StoreInput shop(String slug,long version,boolean accepts) { return new StoreInput("Shop trà",slug,"1 Võ Văn Ngân","0901234567","Trà ngon",LocalTime.of(8,0),LocalTime.of(22,0),accepts,version); }
    ProductInput productInput(Long shop,String slug,long version) { return new ProductInput(shop,categoryId,"Trà sữa",slug,"Mô tả",money(30000),true,true,false,List.of(new VariantInput("M",money(5000),true)),Set.of(toppingId),version); }
    ProductView product() { return catalog.saveProduct(admin,null,productInput(storeId,"tra-sua-test",0)); }
    PromotionInput promo(Long shop,Set<Long> ids,BigDecimal percent,long version) {
        return new PromotionInput(shop,"Ưu đãi","TEST10","PRODUCT_PERCENT",percent,money(100000),money(0),100,1,clock.instant().minusSeconds(60),clock.instant().plusSeconds(3600),true,ids,version);
    }
    private org.springframework.security.core.Authentication auditAdminAuth() {
        return new UsernamePasswordAuthenticationToken(admin,null,List.of(new SimpleGrantedAuthority(admin.role())));
    }
    private void auditPut(String path,Map<String,Object> body,int expectedStatus) throws Exception {
        mvc.perform(put(path).with(authentication(auditAdminAuth())).with(csrf())
            .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().is(expectedStatus));
    }
    @Test void staleToppingFormCannotReenableDisabledTopping() throws Exception {
        String path="/api/admin/catalog/toppings/"+toppingId;
        auditPut(path,Map.of("name","Trân châu","price",5000,"active",false,"version",0),200);
        auditPut(path,Map.of("name","Trân châu","price",6000,"active",true,"version",0),409);
        var current=catalog.toppings(admin).stream().filter(t->t.id().equals(toppingId)).findFirst().orElseThrow();
        assertThat(current.active()).isFalse();assertThat(current.price()).isEqualByComparingTo("5000");
    }
    @Test void staleCategoryFormCannotReenableDisabledCategory() throws Exception {
        String path="/api/admin/catalog/categories/"+categoryId;
        auditPut(path,Map.of("name","Trà sữa","slug","tra-sua","description","","displayOrder",0,"active",false,"version",0),200);
        auditPut(path,Map.of("name","Tên cũ sửa lại","slug","tra-sua","description","","displayOrder",0,"active",true,"version",0),409);
        assertThat(catalog.categories(true,admin).stream().filter(c->c.id().equals(categoryId)).findFirst().orElseThrow().active()).isFalse();
    }
    @Test void staleAreaFormCannotReenableDisabledDeliveryArea() throws Exception {
        var created=stores.area(admin,storeId,null,new AreaInput("AUDIT","Khu vực",true));
        String path="/api/admin/catalog/stores/"+storeId+"/areas/"+created.id();
        auditPut(path,Map.of("areaCode","AUDIT","name","Khu vực","active",false,"version",0),200);
        auditPut(path,Map.of("areaCode","AUDIT","name","Đổi tên cũ","active",true,"version",0),409);
        assertThat(stores.managedStore(admin,storeId).areas().stream().filter(a->a.id().equals(created.id())).findFirst().orElseThrow().active()).isFalse();
    }
    @Test void detachedToppingWriteCannotOverwriteNewerState() throws Exception {
        var stale=em.find(com.utetra.entity.catalog.Topping.class,toppingId);em.detach(stale);
        auditPut("/api/admin/catalog/toppings/"+toppingId,Map.of("name","Trân châu","price",5000,"active",false,"version",0),200);
        assertThatThrownBy(()->em.merge(stale)).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }
    @Test void detachedCategoryWriteCannotOverwriteNewerState() throws Exception {
        var stale=em.find(com.utetra.entity.catalog.Category.class,categoryId);em.detach(stale);
        auditPut("/api/admin/catalog/categories/"+categoryId,Map.of("name","Trà sữa","slug","tra-sua","description","","displayOrder",0,"active",false,"version",0),200);
        assertThatThrownBy(()->em.merge(stale)).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }
    @Test void detachedAreaWriteCannotOverwriteNewerState() throws Exception {
        var area=stores.area(admin,storeId,null,new AreaInput("AUDIT","Khu vực",true));
        var stale=em.find(com.utetra.entity.catalog.StoreDeliveryArea.class,area.id());em.detach(stale);
        auditPut("/api/admin/catalog/stores/"+storeId+"/areas/"+area.id(),Map.of("areaCode","AUDIT","name","Khu vực","active",false,"version",0),200);
        assertThatThrownBy(()->em.merge(stale)).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }
    @Test void configurationUpdateWithoutVersionIsRejected() throws Exception {
        auditPut("/api/admin/catalog/toppings/"+toppingId,Map.of("name","Trân châu","price",6000,"active",false),409);
    }

    @Test void homepageHydratesAllRankingSectionsInOneBoundedBatch() {
        for(int n=0;n<12;n++)catalog.saveProduct(admin,null,productInput(storeId,"home-batch-"+n,0));
        em.flush();em.clear();
        var stats=em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean enabled=stats.isStatisticsEnabled();stats.setStatisticsEnabled(true);stats.clear();
        try {
            var sections=catalog.homeGroups(null);
            assertThat(sections.keySet()).containsExactly("newest","sold","rating","favorites");
            sections.values().forEach(section->{assertThat(section.items()).hasSize(12);section.items().forEach(p->{assertThat(p.variants()).hasSize(1);assertThat(p.toppings()).hasSize(1);});});
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(6);
        } finally {stats.setStatisticsEnabled(enabled);}
    }
    @Test void approvalPromotesBuyerAndRejectsReplay() {
        assertThat(jdbc.queryForObject("select r.name from users u join roles r on r.id=u.role_id where u.id=105",String.class)).isEqualTo("ROLE_VENDOR");
        var current=storeRepo.findById(storeId).orElseThrow();
        assertThatThrownBy(() -> stores.review(admin,storeId,new Decision("APPROVED","Lặp",current.getVersion(),1L))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void approvalRevokesExistingSessionsAndRollbackOnInvalidPolicy() {
        jdbc.update("insert into auth_sessions(user_id,refresh_token_hash,created_at,expires_at,version) values(102,?,current_timestamp,?,0)","a".repeat(64),java.sql.Timestamp.from(Instant.now().plusSeconds(3600)));
        Long pending=stores.register(vendor,shop("second-shop",0,false)).id();
        assertThatThrownBy(() -> stores.review(admin,pending,new Decision("APPROVED","Invalid policy",0,999L))).isInstanceOf(ResponseStatusException.class);
        assertThat(storeRepo.findById(pending).orElseThrow().getStatus()).isEqualTo("PENDING_APPROVAL");
    }
    @Test void scopePolicyUsesDbOwnershipAndMembership() {
        assertThat(policy.canOperate(other,storeId)).isFalse(); assertThat(policy.canOperate(manager,storeId)).isFalse();
        stores.assign(admin,storeId,new Membership(manager.userId(),true),false);
        assertThat(policy.canOperate(manager,storeId)).isTrue();
        assertThatThrownBy(() -> catalog.saveCategory(manager,null,new CategoryInput("Khác","khac","",0,true))).isInstanceOf(ResponseStatusException.class);
        stores.assign(admin,storeId,new Membership(manager.userId(),true),true);
        assertThat(catalog.saveCategory(manager,null,new CategoryInput("Được giao","duoc-giao","",0,true)).id()).isNotNull();
    }
    @Test void applicantsSeeOnlyTheirOwnRegistrationAndReviewFeedback() {
        Long pending=stores.register(vendor,shop("history-shop",0,false)).id();
        stores.review(admin,pending,new Decision("REJECTED","Cần bổ sung địa chỉ",0,null));
        var history=stores.registrations(vendor);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().store().id()).isEqualTo(pending);
        assertThat(history.getFirst().status()).isEqualTo("REJECTED");
        assertThat(history.getFirst().note()).isEqualTo("Cần bổ sung địa chỉ");
        assertThat(history.getFirst().reviewedAt()).isNotNull();
        assertThat(stores.registrations(other)).isEmpty();
        assertThatThrownBy(() -> stores.registrations(null)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void approvalPoliciesAreActiveAndVisibleOnlyToScopedReviewers() {
        jdbc.update("insert into commission_policies(id,name,rate_percent,is_active,created_at,updated_at,version) values(2,'Inactive',7,false,current_timestamp,current_timestamp,0)");
        assertThat(stores.commissionPolicies(admin,storeId)).extracting(CommissionPolicyView::id).containsExactly(1L);
        assertThatThrownBy(() -> stores.commissionPolicies(vendor,storeId)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> stores.commissionPolicies(manager,storeId)).isInstanceOf(ResponseStatusException.class);
        stores.assign(admin,storeId,new Membership(manager.userId(),true),false);
        assertThat(stores.commissionPolicies(manager,storeId)).hasSize(1);
    }
    @Test void cannotTransferProductOrModifyAnotherShop() {
        var p=product(); assertThatThrownBy(() -> catalog.saveProduct(other,p.id(),productInput(storeId,p.slug(),p.version()))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void quoteCalculatesEveryToppingPerCupAndRejectsInvalidOptions() {
        var p=product(); Long variant=p.variants().getFirst().id();
        var quote=catalog.quote(p.id(),new DrinkInput(variant,2,50,70,Set.of(toppingId)));
        assertThat(quote.total()).isEqualByComparingTo("80000");
        assertThatThrownBy(() -> catalog.quote(p.id(),new DrinkInput(variant,1,99,50,Set.of()))).isInstanceOf(ResponseStatusException.class);
        catalog.availability(admin,storeId,toppingId,false);
        assertThatThrownBy(() -> catalog.quote(p.id(),new DrinkInput(variant,1,50,50,Set.of(toppingId)))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void hiddenCategoryAndSuspendedShopCannotBePurchased() {
        var p=product(); catalog.saveCategory(admin,categoryId,new CategoryInput("Trà sữa","tra-sua","",0,false,catalog.categories(true,admin).stream().filter(c->c.id().equals(categoryId)).findFirst().orElseThrow().version()));
        assertThat(catalog.search("",null,null,null,null,"newest",0,12,false,false).total()).isZero();
        assertThatThrownBy(() -> catalog.quote(p.id(),new DrinkInput(p.variants().getFirst().id(),1,100,100,Set.of()))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void shopOpeningHoursAreEnforcedInVietnamTime() {
        var p=product(); var s=storeRepo.findById(storeId).orElseThrow();
        s.setOpenTime(LocalTime.of(18,0)); s.setCloseTime(LocalTime.of(22,0)); storeRepo.flush();
        assertThatThrownBy(() -> catalog.quote(p.id(),new DrinkInput(p.variants().getFirst().id(),1,100,100,Set.of()))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("ngoài giờ");
    }
    @Test void sqlWallClockHoursAreNotShiftedByUtcJdbcConfiguration() {
        jdbc.update("update stores set open_time='08:00:00',close_time='22:00:00' where id=?",storeId); em.clear();
        var shop=stores.publicStore(storeId);
        assertThat(shop.openTime()).isEqualTo(LocalTime.of(8,0)); assertThat(shop.closeTime()).isEqualTo(LocalTime.of(22,0));
    }
    @Test void updatesKeepVariantIdsAndRejectStaleVersions() {
        // A coarse wall clock must not let an unchanged parent bypass versioning of child edits.
        Instant tick=Instant.now();
        try(var wallClock=org.mockito.Mockito.mockStatic(Instant.class,org.mockito.Mockito.CALLS_REAL_METHODS)) {
            wallClock.when(Instant::now).thenReturn(tick);
            var first=product(); var second=catalog.saveProduct(admin,first.id(),productInput(storeId,first.slug(),first.version()));
            assertThat(second.version()).isGreaterThan(first.version()); assertThat(second.variants().getFirst().id()).isEqualTo(first.variants().getFirst().id());
            assertThatThrownBy(() -> catalog.saveProduct(admin,first.id(),productInput(storeId,first.slug(),first.version()))).isInstanceOf(ResponseStatusException.class);
            catalog.disableProduct(admin,second.id(),second.version());
            assertThat(productRepo.existsById(first.id())).isTrue(); assertThat(variantRepo.existsById(first.variants().getFirst().id())).isTrue();
        }
    }
    @Test void validationRejectsNegativePricesAndDuplicateSizes() {
        var bad=new ToppingInput("Sai",money(-1),true);
        assertThatThrownBy(() -> catalog.saveTopping(admin,null,bad)).isInstanceOf(ConstraintViolationException.class);
        var duplicate=new ProductInput(storeId,categoryId,"Món","mon","",money(1),true,true,false,List.of(new VariantInput("M",money(0),true),new VariantInput("M",money(1),true)),Set.of(),0);
        assertThatThrownBy(() -> catalog.saveProduct(admin,null,duplicate)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void promotionScopeMathAndValidation() {
        var p=product(); var promotion=promotions.save(admin,null,promo(storeId,Set.of(p.id()),money(15),0));
        assertThat(promotions.quote(promotion.id(),storeId,Map.of(p.id(),money(80001)),money(20000)).discount()).isEqualByComparingTo("12000");
        assertThatThrownBy(() -> promotions.save(other,null,promo(null,Set.of(),money(10),0))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> promotions.save(admin,null,promo(storeId,Set.of(),money(101),0))).isInstanceOf(ConstraintViolationException.class);
    }
    @Test void guestRankingUsesActualSalesAndTopLimitIsStable() {
        product(); assertThat(catalog.search("",null,null,null,null,"sold",0,12,true,false).total()).isZero();
        assertThatThrownBy(() -> catalog.search("",null,null,null,null,"injected sort",0,12,false,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> catalog.search("",null,null,money(20),money(10),"newest",0,12,false,false)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void actualRankingExcludesUnpaidAndRefundedAndPagesTopTwenty() {
        var p=product(); Long v=p.variants().getFirst().id();
        String[] statuses={"DELIVERED","NEW","DELIVERED","DELIVERED"};
        String[] payments={"PAID","PAID","REFUNDED","UNPAID"};
        for(int n=1;n<=4;n++) {
            TestRows.insert(jdbc,"orders",Map.of("id",n,"status",statuses[n-1],"payment_status",payments[n-1]));
            TestRows.insert(jdbc,"order_items",Map.of("id",n,"order_id",n,"variant_id",v,"quantity",n==1?11:1000));
        }
        TestRows.insert(jdbc,"reviews",Map.of("id",1,"product_id",p.id(),"order_item_id",1,"stars",5,"status","VISIBLE"));
        TestRows.insert(jdbc,"reviews",Map.of("id",2,"product_id",p.id(),"order_item_id",2,"stars",1,"status","HIDDEN"));
        TestRows.insert(jdbc,"product_favorites",Map.of("id",1,"product_id",p.id(),"user_id",103));
        var ranked=catalog.search("",null,null,null,null,"sold",0,12,true,false);
        assertThat(ranked.total()).isEqualTo(1); assertThat(ranked.items().getFirst().soldUnits()).isEqualTo(11);
        assertThat(ranked.items().getFirst().rating()).isEqualByComparingTo("5");
        assertThat(ranked.items().getFirst().favorites()).isEqualTo(1);
        for(int i=0;i<22;i++) catalog.saveProduct(admin,null,productInput(storeId,"top-"+i,0));
        var first=catalog.search("",null,null,null,null,"newest",0,12,false,true);
        var second=catalog.search("",null,null,null,null,"newest",1,12,false,true);
        assertThat(first.total()).isEqualTo(20); assertThat(first.items()).hasSize(12); assertThat(second.items()).hasSize(8);
        assertThat(first.items().stream().map(ProductView::id).toList()).doesNotContainAnyElementsOf(second.items().stream().map(ProductView::id).toList());
    }
    @Test void httpRequiresCsrfAndScopeAndDoesNotSerializeEntities() throws Exception {
        var auth=new UsernamePasswordAuthenticationToken(admin,"",List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mvc.perform(post("/api/admin/catalog/categories").with(authentication(auth)).contentType("application/json").content(mapper.writeValueAsString(new CategoryInput("X","x","",0,true)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/catalog/categories").with(authentication(auth)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(new CategoryInput("X","x","",0,true)))).andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("x")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))));
        mvc.perform(get("/products/lookups")).andExpect(status().isOk()).andExpect(jsonPath("$.categories[0].name").value("Trà sữa"));
    }
}
