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

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:catalog_workflow_regression;MODE=MySQL;DB_CLOSE_DELAY=-1") @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CatalogWorkflowRegressionTest {
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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void storeUpdateRequiresExplicitVersionEvenAtZero(boolean explicitNull) throws Exception {
        var fresh=stores.register(vendor,shop("version-zero",0,false));
        Map<String,Object> body=mapper.convertValue(shop("version-zero",0,false),Map.class);
        body.put("description","Must not save");
        if(explicitNull) body.put("version",null); else body.remove("version");
        putBody("/api/admin/catalog/stores/"+fresh.id(),body,409);
        assertThat(stores.managedStore(admin,fresh.id()).description()).isEqualTo("Trà ngon");
        assertThat(stores.managedStore(admin,fresh.id()).version()).isZero();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void promotionUpdateRequiresExplicitVersionEvenAtZero(boolean explicitNull) throws Exception {
        var fresh=promotions.save(admin,null,promo(storeId,Set.of(),money(10),0));
        Map<String,Object> body=mapper.convertValue(promo(storeId,Set.of(),money(20),0),Map.class);
        if(explicitNull) body.put("version",null); else body.remove("version");
        putBody("/api/admin/catalog/promotions/"+fresh.id(),body,409);
        assertThat(promotions.managed(admin,storeId).getFirst().percent()).isEqualByComparingTo("10");
        assertThat(promotions.managed(admin,storeId).getFirst().version()).isZero();
    }
    @Test void createsMayOmitVersionAndExplicitZeroUpdateStillRejectsStaleWrites() throws Exception {
        Map<String,Object> shopBody=mapper.convertValue(shop("no-version-create",0,false),Map.class);
        shopBody.remove("version");
        var created=stores.register(vendor,mapper.convertValue(shopBody,StoreInput.class));
        shopBody.put("version",0);shopBody.put("description","Saved once");
        putBody("/api/admin/catalog/stores/"+created.id(),shopBody,200);
        shopBody.put("description","Stale write");putBody("/api/admin/catalog/stores/"+created.id(),shopBody,409);
        assertThat(stores.managedStore(admin,created.id()).description()).isEqualTo("Saved once");
        Map<String,Object> promoBody=mapper.convertValue(promo(storeId,Set.of(),money(10),0),Map.class);
        promoBody.remove("version");
        var offer=promotions.save(admin,null,mapper.convertValue(promoBody,PromotionInput.class));
        promoBody.put("version",0);promoBody.put("percent",20);
        putBody("/api/admin/catalog/promotions/"+offer.id(),promoBody,200);
        promoBody.put("percent",30);putBody("/api/admin/catalog/promotions/"+offer.id(),promoBody,409);
        assertThat(promotions.managed(admin,storeId).getFirst().percent()).isEqualByComparingTo("20");
    }
    @Test void unrelatedProductUpdatePreservesInactiveCategoryAndPublicVisibility() {
        var item=product();
        catalog.saveCategory(admin,categoryId,new CategoryInput("Trà sữa","tra-sua","",0,false,0L));
        var input=new ProductInput(storeId,categoryId,item.name(),item.slug(),"Updated text",money(35000),true,true,false,
            List.of(new VariantInput("M",money(5000),true)),Set.of(toppingId),item.version());
        var saved=catalog.saveProduct(vendorForOwner(),item.id(),input);
        assertThat(saved.categoryId()).isEqualTo(categoryId);
        assertThat(saved.basePrice()).isEqualByComparingTo("35000");
        em.flush();em.clear();
        assertThat(productRepo.findById(item.id()).orElseThrow().getCategoryId()).isEqualTo(categoryId);
        assertThatThrownBy(()->catalog.detail(item.id())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void cannotCreateOrReassignProductToInactiveCategory() {
        var item=product();
        var inactive=catalog.saveCategory(admin,null,new CategoryInput("Disabled","disabled","",1,false));
        var input=new ProductInput(storeId,inactive.id(),item.name(),"new-inactive",item.description(),money(35000),true,true,false,
            List.of(new VariantInput("M",money(5000),true)),Set.of(toppingId),item.version());
        assertThatThrownBy(()->catalog.saveProduct(admin,null,input)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->catalog.saveProduct(admin,item.id(),input)).isInstanceOf(ResponseStatusException.class);
        assertThat(productRepo.findById(item.id()).orElseThrow().getCategoryId()).isEqualTo(categoryId);
    }
    @Test void storeUnrelatedUpdatePreservesSeconds() {
        var body=new StoreInput("Shop trà","seconds","1 Võ Văn Ngân","0901234567","Original",LocalTime.of(8,0,1),LocalTime.of(23,59,59),false,0);
        var created=stores.register(vendor,body);
        var changed=new StoreInput(body.name(),body.slug(),body.address(),body.phone(),"Changed",body.openTime(),body.closeTime(),false,0);
        stores.update(admin,created.id(),changed);em.flush();em.clear();
        var saved=stores.managedStore(admin,created.id());
        assertThat(saved.openTime()).isEqualTo(LocalTime.of(8,0,1));
        assertThat(saved.closeTime()).isEqualTo(LocalTime.of(23,59,59));
        assertThat(saved.description()).isEqualTo("Changed");
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void storeRegistrationRejectsFractionalSeconds(boolean fractionOnOpen) {
        var input=fractionalShop(fractionOnOpen);
        assertThatThrownBy(()->stores.register(vendor,input))
            .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThat(storeRepo.findAll()).noneMatch(s->s.getSlug().equals("fractional"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void storeUpdateRejectsFractionalSeconds(boolean fractionOnOpen) {
        var original=stores.register(vendor,shop("fractional",0,false));
        assertThatThrownBy(()->stores.update(admin,original.id(),fractionalShop(fractionOnOpen)))
            .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(400));
        em.flush();em.clear();
        var unchanged=stores.managedStore(admin,original.id());
        assertThat(unchanged.openTime()).isEqualTo(LocalTime.of(8,0));
        assertThat(unchanged.closeTime()).isEqualTo(LocalTime.of(22,0));
        assertThat(unchanged.version()).isZero();
    }
    private StoreInput fractionalShop(boolean fractionOnOpen) {
        return new StoreInput("Shop trà","fractional","1 Võ Văn Ngân","0901234567","Fractional",
            LocalTime.of(8,0,0,fractionOnOpen?1:0),LocalTime.of(22,0,0,fractionOnOpen?0:123000000),false,0);
    }
    @Test void adminCanTransferExistingStoreOwnershipAndOldOwnerLosesAccess() throws Exception {
        long version=stores.managedStore(admin,storeId).version();
        assertThat(stores.managedStore(buyer,storeId).id()).isEqualTo(storeId);
        putBody("/api/admin/catalog/stores/"+storeId+"/owner",Map.of("userId",vendor.userId(),"version",version),200);
        em.clear();
        assertThat(stores.managedStore(admin,storeId).ownerUserId()).isEqualTo(vendor.userId());
        assertThat(policy.canOperate(vendor,storeId)).isTrue();
        assertThatThrownBy(()->stores.managedStore(buyer,storeId)).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='TRANSFER_OWNER' and entity_id=?",Integer.class,storeId)).isEqualTo(1);
        putBody("/api/admin/catalog/stores/"+storeId+"/owner",Map.of("userId",other.userId(),"version",version),409);
        assertThat(stores.managedStore(admin,storeId).ownerUserId()).isEqualTo(vendor.userId());
    }
    @Test void ownershipChangeRejectsInvalidTargetMissingVersionAndOtherRoles() throws Exception {
        long version=stores.managedStore(admin,storeId).version();
        String path="/api/admin/catalog/stores/"+storeId+"/owner";
        putBody(path,Map.of("userId",manager.userId(),"version",version),400);
        jdbc.update("update users set is_active=false where id=?",other.userId());
        putBody(path,Map.of("userId",other.userId(),"version",version),400);
        putBody(path,Map.of("userId",vendor.userId()),400);
        var auth=new UsernamePasswordAuthenticationToken(vendor,null,List.of(new SimpleGrantedAuthority(vendor.role())));
        mvc.perform(put("/api/vendor/catalog/stores/"+storeId+"/owner").with(authentication(auth)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(Map.of("userId",vendor.userId(),"version",version)))).andExpect(status().isForbidden());
        em.clear();assertThat(stores.managedStore(admin,storeId).ownerUserId()).isEqualTo(buyer.userId());
    }
    @Test void editingManagerAssignmentMovesOnlyTheSelectedAssignment() throws Exception {
        stores.assign(admin,storeId,new Membership(manager.userId(),true),false);
        var second=stores.register(vendor,shop("second-assignment",0,false));
        var body=Map.of("userId",manager.userId(),"active",true,"previousUserId",manager.userId(),"previousStoreId",storeId);
        var auth=new UsernamePasswordAuthenticationToken(admin,null,List.of(new SimpleGrantedAuthority(admin.role())));
        mvc.perform(post("/api/admin/catalog/stores/"+second.id()+"/memberships").with(authentication(auth)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body))).andExpect(status().isOk());
        em.flush();em.clear();
        assertThat(policy.canOperate(manager,storeId)).isFalse();
        assertThat(policy.canOperate(manager,second.id())).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from store_memberships where user_id=? and is_active=true",Integer.class,manager.userId())).isEqualTo(1);
    }
    @Test void movingAssignmentToExistingActiveTargetIsRejectedWithoutRevokingSource() throws Exception {
        stores.assign(admin,storeId,new Membership(manager.userId(),true),false);
        var second=stores.register(vendor,shop("occupied-target",0,false));
        stores.assign(admin,second.id(),new Membership(manager.userId(),true),false);
        var body=Map.of("userId",manager.userId(),"active",true,"previousUserId",manager.userId(),"previousStoreId",storeId);
        var auth=new UsernamePasswordAuthenticationToken(admin,null,List.of(new SimpleGrantedAuthority(admin.role())));
        mvc.perform(post("/api/admin/catalog/stores/"+second.id()+"/memberships").with(authentication(auth)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        em.clear();assertThat(policy.canOperate(manager,storeId)).isTrue();assertThat(policy.canOperate(manager,second.id())).isTrue();
    }
    @Test void vendorPickerReturnsStableKeysAndOnlyEligibleAccounts() throws Exception {
        jdbc.update("update users set is_active=false where id=?",other.userId());
        var auth=new UsernamePasswordAuthenticationToken(admin,null,List.of(new SimpleGrantedAuthority(admin.role())));
        mvc.perform(get("/api/admin/catalog/vendors").with(authentication(auth))).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id",org.hamcrest.Matchers.hasItem(vendor.userId().intValue())))
            .andExpect(jsonPath("$[*].id",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(other.userId().intValue()))))
            .andExpect(jsonPath("$[0].name").isString());
    }
    private UtetraPrincipal vendorForOwner() { return buyer; }
    private void putBody(String path,Map<String,Object> body,int expected) throws Exception {
        var auth=new UsernamePasswordAuthenticationToken(admin,null,List.of(new SimpleGrantedAuthority(admin.role())));
        mvc.perform(put(path).with(authentication(auth)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body))).andExpect(status().is(expected));
    }
}
