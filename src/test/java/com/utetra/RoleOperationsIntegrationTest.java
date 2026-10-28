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
class RoleOperationsIntegrationTest {
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
    @Autowired com.utetra.identity.UserManagementService management;
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

    @BeforeEach void assignment() {
        jdbc.update("insert into store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at) values(?,?,'CATALOG_ADMIN',true,?,current_timestamp,current_timestamp)",manager.userId(),storeId,admin.userId());
    }
    @Test void delegatedManagerCannotEditProductPriceOrCreatePromotion() {
        var item=product();
        assertThatThrownBy(()->catalog.saveProduct(manager,item.id(),productInput(storeId,"tra-sua-test",item.version())))
            .isInstanceOf(ResponseStatusException.class).satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(()->promotions.save(manager,null,promo(storeId,Set.of(),money(10),0)))
            .isInstanceOf(ResponseStatusException.class).satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(403));
        assertThat(productRepo.findById(item.id()).orElseThrow().getBasePrice()).isEqualByComparingTo(money(30000));
    }
    @Test void managerOperationsChangeAvailabilityButCannotSupplyPrice() throws Exception {
        var item=product();var auth=new UsernamePasswordAuthenticationToken(manager,null,List.of(new SimpleGrantedAuthority(manager.role())));
        mvc.perform(put("/api/manager/operations/products/"+item.id()+"/availability").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"available\":false,\"version\":"+item.version()+"}"))
            .andExpect(status().isOk());
        em.clear();assertThat(productRepo.findById(item.id()).orElseThrow().getAvailable()).isFalse();
        mvc.perform(put("/api/manager/operations/products/"+item.id()+"/availability").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"available\":true,\"version\":"+item.version()+"}"))
            .andExpect(status().isConflict());
        jdbc.update("update store_memberships set is_active=false where user_id=?",manager.userId());
        mvc.perform(put("/api/manager/operations/products/"+item.id()+"/availability").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"available\":true,\"version\":1}"))
            .andExpect(status().isForbidden());
    }
    @Test void managerCannotChangeStoreProfileEvenWhenDelegated() {
        var s=storeRepo.findById(storeId).orElseThrow();
        assertThatThrownBy(()->stores.update(manager,storeId,shop("shop-test",s.getVersion(),true))).isInstanceOf(ResponseStatusException.class)
            .satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(403));
    }
    @Test void loginEntryPointPreservesRoleDeepLinkAndFilter() throws Exception {
        var result=mvc.perform(get("/manager/operations?store=1").servletPath("/manager/operations").accept("text/html")).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getHeader("Location")).contains("next=%2Fmanager%2Foperations%3Fstore%3D1");
    }
    @Test void adminCanRevokeDisabledManagerWithoutGrantingAccess() {
        jdbc.update("update users set is_active=false where id=?",manager.userId());
        assertThatCode(()->stores.assign(admin,storeId,new Membership(manager.userId(),false),false)).doesNotThrowAnyException();
        assertThat(jdbc.queryForObject("select is_active from store_memberships where user_id=? and store_id=?",Boolean.class,manager.userId(),storeId)).isFalse();
    }
    @Test void identityRegrantRequiresExplicitCatalogCapabilityDecision() {
        jdbc.update("update store_memberships set is_active=false where user_id=? and store_id=?",manager.userId(),storeId);
        assertThatThrownBy(()->management.assign(admin,manager.userId(),storeId,true)).isInstanceOf(com.utetra.identity.IdentityException.class);
        assertThat(jdbc.queryForObject("select is_active from store_memberships where user_id=? and store_id=?",Boolean.class,manager.userId(),storeId)).isFalse();
    }
    @Test void adminDirectoryUsesStableCamelCaseJsonContract() throws Exception {
        var auth=new UsernamePasswordAuthenticationToken(admin,null,List.of(new SimpleGrantedAuthority(admin.role())));
        var response=mvc.perform(get("/api/admin/operations/managers").with(authentication(auth))).andExpect(status().isOk()).andReturn();
        var data=mapper.readTree(response.getResponse().getContentAsString());
        assertThat(data.path("managers").get(0).has("id")).isTrue();assertThat(data.path("managers").get(0).has("name")).isTrue();
        assertThat(data.path("stores").get(0).has("id")).isTrue();
    }
}
