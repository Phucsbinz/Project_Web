package com.utetra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Real Tomcat requests exercise JSP compilation and SiteMesh forwards. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BaselineViewTest {
    @Autowired TestRestTemplate client;

    @Test void homeRendersThroughTheClientDecorator() {
        var response = client.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UTETra", "navbar-brand", "site-footer")
            .doesNotContain("<sitemesh:write", "<c:forEach", "<%@");
        assertThat(response.getBody().split("navbar-brand", -1)).hasSize(2);
    }

    @Test void loginRendersThroughTheAuthDecorator() {
        var response = client.getForEntity("/auth/login", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UTETra", "<!DOCTYPE html>")
            .doesNotContain("<sitemesh:write", "<%@");
    }

    @Test void apiAndAssetsRemainUndecorated() {
        var health = client.getForEntity("/api/system/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"application\":\"UTETra\"")
            .doesNotContain("<!DOCTYPE", "navbar-brand");
        var asset = client.getForEntity("/assets/vendor/bootstrap/bootstrap.min.css", String.class);
        assertThat(asset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asset.getBody()).contains("--bs-blue:", ".navbar").doesNotContain("<!DOCTYPE");
    }
}
