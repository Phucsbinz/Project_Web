package com.utetra;

import com.utetra.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Real embedded Tomcat compiles JSPs, JSTL fragments and each role's SiteMesh layout. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:catalog_views;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class CatalogViewTest {
    @Autowired TestRestTemplate client;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @BeforeAll static void fixture(@Autowired JdbcTemplate jdbc) {
        for(String role:List.of("ADMIN","VENDOR","MANAGER","USER")) jdbc.update("insert into roles(name) values(?)","ROLE_"+role);
        jdbc.update("insert into users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version) values(1,(select id from roles where name='ROLE_VENDOR'),'view-test','view@example.test','no-login','View test',true,current_timestamp,current_timestamp,current_timestamp,0)");
        jdbc.update("insert into stores(id,owner_user_id,commission_policy_id,name,slug,address,phone,open_time,close_time,status,accepting_orders,created_at,updated_at,version) values(1,1,1,'Shop demo','shop-demo','Thủ Đức','0901234567','08:00','22:00','ACTIVE',true,current_timestamp,current_timestamp,0)");
        jdbc.update("insert into categories(id,name,slug,display_order,is_active,created_at,updated_at) values(1,'Trà sữa','tra-sua',0,true,current_timestamp,current_timestamp)");
        jdbc.update("insert into products(id,store_id,category_id,name,slug,base_price,is_active,is_available,is_featured,created_at,updated_at,version) values(1,1,1,'Trà <script>alert(1)</script>','tra-test',30000,true,true,false,current_timestamp,current_timestamp,0)");
        jdbc.update("insert into product_variants(product_id,size_name,extra_price,is_active,created_at,updated_at) values(1,'M',5000,true,current_timestamp,current_timestamp)");
        jdbc.update("insert into auth_sessions(id,user_id,refresh_token_hash,created_at,expires_at,version) values(1,1,?,current_timestamp,?,0)","b".repeat(64),java.sql.Timestamp.from(Instant.now().plusSeconds(3600)));
    }
    @Test void quickViewAllowsSameOriginAndNormalProductKeepsFrameProtection() {
        assertThat(client.getForEntity("/products/1?quick=1",String.class).getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(client.getForEntity("/products/1",String.class).getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(client.getForObject("/products/1",String.class)).contains("Thêm vào giỏ hàng","product-community.js");
    }
    @Test void publicPagesRenderEscapedDataAndNoJspSource() {
        for(String path:List.of("/","/menu","/products/1","/stores","/stores/1")) {
            var response=client.getForEntity(path,String.class);
            assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).as(path).contains("navbar-brand","site-footer").doesNotContain("<%@","<c:forEach","<script>alert(1)</script>");
        }
        assertThat(client.getForObject("/products/1",String.class)).contains("&lt;script&gt;","drink-form","sugarLevel");
    }
    @Test void authenticatedHomeAndRegistrationAndAllManagementLayoutsCompile() {
        for(String role:List.of("VENDOR","MANAGER","ADMIN")) {
            jdbc.update("update users set role_id=(select id from roles where name=?) where id=1","ROLE_"+role);
            var headers=new HttpHeaders(); headers.setBearerAuth(jwt.issue(1L,1L,"ROLE_"+role));
            String path="/"+role.toLowerCase()+"/products";
            var response=client.exchange(path,HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("catalog-manager","catalog-management.js","_csrf","data-section=\"products\"").doesNotContain("<%@","<sitemesh:write");
        }
        jdbc.update("update users set role_id=(select id from roles where name='ROLE_VENDOR') where id=1");
        var headers=new HttpHeaders(); headers.setBearerAuth(jwt.issue(1L,1L,"ROLE_VENDOR"));
        var home=client.exchange("/",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        assertThat(home.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(home.getBody()).contains("catalog-homepage","Xem thực đơn","href=\"/menu\"","home-bestsellers","home-new","home-branches","drink-modal","header-search","Shop demo")
            .doesNotContain("catalog-filters","catalog-group","Bốn bảng xếp hạng sản phẩm");
        for(String group:List.of("newest","sold","rating","favorites")) {
            var menu=client.exchange("/menu?top=true&sort="+group,HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(menu.getStatusCode()).as(group).isEqualTo(HttpStatus.OK);
            assertThat(menu.getBody()).contains("20 món mới nhất","20 món bán chạy","20 món đánh giá cao","20 món được yêu thích","name=\"top\" value=\"true\"");
        }
        var registration=client.exchange("/account/shop-registration",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.OK); assertThat(registration.getBody()).contains("registration-form");
    }
    @Test void homepageUsesTheGuestContentForEveryAuthenticatedRole() {
        String guest=homepageContent(client.getForObject("/",String.class));
        for(String role:List.of("USER","VENDOR","MANAGER","ADMIN")) {
            jdbc.update("update users set role_id=(select id from roles where name=?) where id=1","ROLE_"+role);
            var headers=new HttpHeaders();headers.setBearerAuth(jwt.issue(1L,1L,"ROLE_"+role));
            var response=client.exchange("/",HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(response.getStatusCode()).as(role).isEqualTo(HttpStatus.OK);
            assertThat(homepageContent(response.getBody())).as(role+" homepage content").isEqualTo(guest);
        }
    }
    private String homepageContent(String html) {
        int start=html.indexOf("<div class=\"catalog-homepage\">");
        int end=html.indexOf("<div class=\"modal fade\" id=\"drink-modal\"",start);
        assertThat(start).isGreaterThanOrEqualTo(0);assertThat(end).isGreaterThan(start);
        return html.substring(start,end);
    }
    @Test void homepageCapsEachCarouselAtTwentyWithTheCorrectRanking() {
        try {
            TestRows.insert(jdbc,"orders",java.util.Map.of("id",500,"user_id",1,"store_id",1,"status","DELIVERED","payment_status","PAID"));
            for(int n=1;n<=22;n++) {
                int id=500+n;
                TestRows.insert(jdbc,"products",java.util.Map.of("id",id,"store_id",1,"category_id",1,"name","Carousel "+n,
                    "slug","carousel-"+n,"base_price",30000,"is_active",true,"is_available",true,
                    "created_at",java.sql.Timestamp.from(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(n))));
                TestRows.insert(jdbc,"product_variants",java.util.Map.of("id",id,"product_id",id,"size_name","M","extra_price",0,"is_active",true));
                TestRows.insert(jdbc,"order_items",java.util.Map.of("id",id,"order_id",500,"variant_id",id,"quantity",100-n));
            }
            String html=client.getForObject("/",String.class);
            String best=html.substring(html.indexOf("id=\"home-bestsellers\""),html.indexOf("id=\"home-new\""));
            String newest=html.substring(html.indexOf("id=\"home-new\""),html.indexOf("class=\"home-selection-note\""));
            for(String pane:List.of(best,newest)) {
                assertThat(java.util.regex.Pattern.compile("<article class=\"card h-100 product-card\">").matcher(pane).results().count()).isEqualTo(20);
                assertThat(pane).contains("home-product-track","data-carousel-next","Xem thêm");
            }
            assertThat(best).contains("/products/501").doesNotContain("/products/522");
            assertThat(newest).contains("/products/522").doesNotContain("/products/501");
        } finally {
            jdbc.update("delete from order_items where id between 501 and 522");
            jdbc.update("delete from orders where id=500");
            jdbc.update("delete from product_variants where id between 501 and 522");
            jdbc.update("delete from products where id between 501 and 522");
        }
    }
    @Test void realCookieCsrfRoundTripAllowsAdminCreateWithoutMockSecurity() {
        jdbc.update("update users set role_id=(select id from roles where name='ROLE_ADMIN') where id=1");
        var csrf=client.getForEntity("/products/csrf",java.util.Map.class);
        assertThat(csrf.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cookie=csrf.getHeaders().get(HttpHeaders.SET_COOKIE).stream().filter(x -> x.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow().split(";",2)[0];
        var headers=new HttpHeaders(); headers.setBearerAuth(jwt.issue(1L,1L,"ROLE_ADMIN")); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE,cookie); headers.set(csrf.getBody().get("headerName").toString(),csrf.getBody().get("token").toString());
        var response=client.exchange("/api/admin/catalog/categories",HttpMethod.POST,new HttpEntity<>("{\"name\":\"Browser category\",\"slug\":\"browser-category\",\"description\":\"\",\"displayOrder\":0,\"active\":true}",headers),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK); assertThat(response.getBody()).contains("browser-category");
    }
    @Test void applicantHistoryRendersEscapedFeedbackOnActualTomcat() {
        jdbc.update("update users set role_id=(select id from roles where name='ROLE_VENDOR') where id=1");
        jdbc.update("insert into store_registration_requests(store_id,applicant_id,status,note,created_at,version) values(1,1,'REJECTED','<script>history-xss</script>',current_timestamp,0)");
        try {
            var headers=new HttpHeaders(); headers.setBearerAuth(jwt.issue(1L,1L,"ROLE_VENDOR"));
            var response=client.exchange("/account/shop-registration",HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Hồ sơ bị từ chối","&lt;script&gt;history-xss&lt;/script&gt;","Đường dẫn","Điện thoại","Đóng cửa").doesNotContain("<script>history-xss</script>");
        } finally { jdbc.update("delete from store_registration_requests where applicant_id=1"); }
    }
    @Test void shopMenuCanReachProductsBeyondFirstPageAndTopFilterIsRetained() {
        for(int i=101;i<=113;i++) jdbc.update("insert into products(id,store_id,category_id,name,slug,base_price,is_active,is_available,is_featured,created_at,updated_at,version) values(?,1,1,?,?,30000,true,true,false,current_timestamp,current_timestamp,0)",i,"Paging tea "+i,"paging-tea-"+i);
        try {
            var first=client.getForEntity("/stores/1",String.class);
            var second=client.getForEntity("/stores/1?page=1",String.class);
            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(first.getBody()).contains("Trang sau","14 món","data-page=\"1\"");
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(second.getBody()).contains("Trang trước","Trang 2").doesNotContain("Trang sau");
            assertThat(client.getForObject("/menu?top=true&sort=rating",String.class)).contains("name=\"top\" value=\"true\"");
            assertThat(client.getForEntity("/stores/1?page=-1",String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        } finally { jdbc.update("delete from products where id between 101 and 113"); }
    }
}
