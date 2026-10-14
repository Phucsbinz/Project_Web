package com.utetra.order;

import com.utetra.UtetraApplication;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.security.JwtService;
import java.nio.file.*;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Test-only browser smoke harness. Never included in the production WAR. */
public final class OrderSmokeHarness {
  public static void main(String[] args) throws Exception {
    SpringApplication app =
        new SpringApplication(
            UtetraApplication.class, OrderCommerceIntegrationTest.TimeConfiguration.class);
    app.setAdditionalProfiles("test");
    app.setDefaultProperties(
        Map.of(
            "server.port",
            "8082",
            "features.shipper.enabled",
            "true",
            "spring.config.additional-location",
            "file:./src/test/resources/"));
    ConfigurableApplicationContext context = app.run(args);
    var fixture = new OrderCommerceIntegrationTest();
    fixture.jdbc = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
    fixture.time = context.getBean(OrderCommerceIntegrationTest.MutableClock.class);
    fixture.passwords =
        context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
    fixture.carts = context.getBean(com.utetra.order.service.CartService.class);
    fixture.fixture();
    fixture.carts.add(OrderCommerceIntegrationTest.BUYER, fixture.item(2));
    String token = context.getBean(JwtService.class).issue(1L, 1L, "ROLE_USER");
    Path file = Path.of("/tmp/utetra-browser-test-token");
    Files.writeString(file, token);
    Files.setPosixFilePermissions(
        file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    Map<String, String> tokens = new java.util.LinkedHashMap<>();
    for (var actor :
        java.util.List.of(
            OrderCommerceIntegrationTest.BUYER,
            OrderCommerceIntegrationTest.VENDOR,
            OrderCommerceIntegrationTest.MANAGER,
            OrderCommerceIntegrationTest.ADMIN,
            OrderCommerceIntegrationTest.SHIPPER))
      tokens.put(
          actor.role(),
          context.getBean(JwtService.class).issue(actor.userId(), actor.userId(), actor.role()));
    Path tokensFile = Path.of("/tmp/utetra-order-smoke-tokens.json");
    Files.writeString(
        tokensFile, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(tokens));
    Files.setPosixFilePermissions(
        tokensFile, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    System.out.println("TEST_BROWSER_FIXTURES_READY");
  }
}
