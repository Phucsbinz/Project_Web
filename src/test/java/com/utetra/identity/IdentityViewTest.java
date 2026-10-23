package com.utetra.identity;
import static com.utetra.identity.IdentityRepository.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Exercises JSP compilation on Tomcat; MockMvc does not execute JSPs. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdentityViewTest {
    @Autowired TestRestTemplate http;
    @Autowired IdentityRepository db;
    @Autowired AuthService auth;
    @Autowired OtpService otp;
    @MockitoBean OtpDelivery delivery;
    @Test void publicIdentityPagesRenderFormsWithoutJspOrDecoratorErrors() {
        for(String path:List.of("/auth/login","/auth/register","/auth/otp","/auth/forgot","/auth/reset")) {
            var res=http.getForEntity(path,String.class);
            assertThat(res.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            assertThat(res.getHeaders().get("Set-Cookie")).as("CSRF cookie must be emitted before SiteMesh commits headers").anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN="));
            assertThat(res.getBody()).contains("<form","identity-message","identity.js","Vị trà thân quen.")
                .doesNotContain("<c:","<%@","<sitemesh:","Vá»", "trÃ");
        }
    }
    @Test void authenticatedIdentityPagesRenderOnTomcat() {
        if(db.count("select count(*) from roles where name='ROLE_USER'")==0) db.update("insert into roles(name) values('ROLE_USER')");
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,12),email="view"+suffix+"@example.test";
        var code=new AtomicReference<String>();
        doAnswer(call -> { code.set(call.getArgument(1)); return null; }).when(delivery).send(anyString(),anyString(),anyString());
        auth.register(new IdentityForms.Register("view"+suffix,email,"ViewPassword42!","Người dùng giao diện")); otp.verify(email,"REGISTER",code.get());
        var tokens=auth.login(email,"ViewPassword42!"); var headers=new HttpHeaders(); headers.setBearerAuth(tokens.access());
        for(String path:List.of("/account/profile","/account/security","/account/addresses","/account/loyalty","/favorites","/viewed","/reviews","/comments")) {
            var res=http.exchange(path,HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(res.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody()).contains("identity-content","identity.js").doesNotContain("<c:","<%@","<sitemesh:");
            if(path.equals("/account/security")) assertThat(res.getBody()).contains("Mật khẩu và bảo mật","Quên mật khẩu hiện tại?","/auth/forgot","confirmPassword");
        }
        var csrfPage=http.exchange("/account/addresses",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        var matcher=java.util.regex.Pattern.compile("name=\"_csrf\" content=\"([^\"]+)\"").matcher(csrfPage.getBody());
        assertThat(matcher.find()).isTrue(); String masked=matcher.group(1);
        String csrfCookie=csrfPage.getHeaders().get("Set-Cookie").stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow().split(";",2)[0];
        headers.set("Cookie",csrfCookie);
        var data=http.exchange("/account/profile/data",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.getHeaders().getOrDefault("Set-Cookie",List.of())).noneMatch(c -> c.startsWith("XSRF-TOKEN="));
        headers.setContentType(MediaType.APPLICATION_JSON); headers.set("X-XSRF-TOKEN",masked);
        var saved=http.exchange("/account/addresses",HttpMethod.POST,new HttpEntity<>(Map.of("recipientName","Kiểm tra CSRF","phone","0901234567","addressDetail","TP HCM","isDefault",true),headers),String.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
