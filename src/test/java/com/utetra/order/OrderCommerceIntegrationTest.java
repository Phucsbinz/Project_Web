package com.utetra.order;

import static org.assertj.core.api.Assertions.*;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.port.OrderLoyaltyBridge;
import com.utetra.order.repository.*;
import com.utetra.order.service.*;
import com.utetra.security.JwtService;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "features.shipper.enabled=true")
@ActiveProfiles("test")
@Import(OrderCommerceIntegrationTest.TimeConfiguration.class)
class OrderCommerceIntegrationTest {
  @Autowired CartService carts;
  @Autowired GuestCartService guests;
  @Autowired CheckoutService checkout;
  @Autowired OrderWorkflowService workflow;
  @Autowired OperationsService operations;
  @Autowired OrderLoyaltyBridge points;
  @Autowired JdbcTemplate jdbc;
  @Autowired TestRestTemplate http;
  @Autowired JwtService jwt;
  @Autowired MutableClock time;
  @Autowired PasswordEncoder passwords;
  static final Actor BUYER = new Actor(1, "ROLE_USER"),
      BUYER_B = new Actor(6, "ROLE_USER"),
      VENDOR = new Actor(2, "ROLE_VENDOR"),
      MANAGER = new Actor(3, "ROLE_MANAGER"),
      ADMIN = new Actor(4, "ROLE_ADMIN"),
      SHIPPER = new Actor(5, "ROLE_SHIPPER");

  static class MutableClock extends Clock {
    final AtomicReference<Instant> current =
        new AtomicReference<>(Instant.parse("2026-10-14T03:00:00Z"));

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return current.get();
    }

    void advance(Duration duration) {
      current.updateAndGet(i -> i.plus(duration));
    }
  }

  @TestConfiguration
  static class TimeConfiguration {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }

  @BeforeEach
  void fixture() {
    jdbc.execute(
        (org.springframework.jdbc.core.ConnectionCallback<Void>)
            connection -> {
              String url = connection.getMetaData().getURL();
              boolean memory = url.startsWith("jdbc:h2:mem:utetra_");
              boolean disposableMysql =
                  "true".equals(System.getenv("ORDER_MYSQL_TEST_ALLOW_RESET"))
                      && url.matches(
                          "jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/utetra_order_[a-z0-9_]+(?:\\?.*)?");
              if (!memory && !disposableMysql)
                throw new IllegalStateException(
                    "Test fixtures may only reset the isolated in-memory DB or explicitly enabled"
                        + " local utetra_order_* MySQL DB");
              return null;
            });
    new ResourceDatabasePopulator(new ClassPathResource("order/reference-schema.sql"))
        .execute(Objects.requireNonNull(jdbc.getDataSource()));
    jdbc.execute(
        (org.springframework.jdbc.core.ConnectionCallback<Void>)
            connection -> {
              boolean mysql = connection.getMetaData().getDatabaseProductName().equals("MySQL");
              try (var statement = connection.createStatement()) {
                statement.execute(
                    mysql ? "SET FOREIGN_KEY_CHECKS=0" : "SET REFERENTIAL_INTEGRITY FALSE");
                try {
                  List<String> tables = new ArrayList<>();
                  try (var result =
                      statement.executeQuery(
                          mysql
                              ? "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE"
                                  + " TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'"
                              : "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE"
                                  + " TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE'")) {
                    while (result.next()) tables.add(result.getString(1));
                  }
                  for (String table : tables) {
                    if (table.equalsIgnoreCase("flyway_schema_history")) continue;
                    if (!table.matches("[a-zA-Z0-9_]+"))
                      throw new IllegalStateException("Unexpected table name");
                    statement.executeUpdate("DELETE FROM " + table);
                  }
                } finally {
                  statement.execute(
                      mysql ? "SET FOREIGN_KEY_CHECKS=1" : "SET REFERENTIAL_INTEGRITY TRUE");
                }
              }
              return null;
            });
    time.current.set(Instant.parse("2026-10-14T03:00:00Z"));
    for (int i = 1; i <= 5; i++)
      jdbc.update(
          "insert into roles(id,name) values(?,?)",
          i,
          List.of("ROLE_USER", "ROLE_VENDOR", "ROLE_MANAGER", "ROLE_ADMIN", "ROLE_SHIPPER")
              .get(i - 1));
    String password = passwords.encode("Only-test-user-password");
    for (int i = 1; i <= 6; i++) {
      jdbc.update(
          "insert into"
              + " users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version)"
              + " values(?,?,?,?,?,?,true,?,?,?,0)",
          i,
          i == 6 ? 1 : i,
          "user" + i,
          "u" + i + "@test.invalid",
          password,
          "User " + i,
          time.instant(),
          time.instant(),
          time.instant());
      jdbc.update(
          "insert into auth_sessions(id,user_id,refresh_token_hash,created_at,expires_at,version)"
              + " values(?,?,?,?,?,0)",
          i,
          i,
          "hash" + i,
          time.instant(),
          time.instant().plus(Duration.ofDays(7)));
    }
    jdbc.update(
        "insert into"
            + " commission_policies(id,name,rate_percent,is_active,created_at,updated_at,version)"
            + " values(1,'Default',10,true,?,?,0)",
        time.instant(),
        time.instant());
    for (int id = 1; id <= 2; id++)
      jdbc.update(
          "insert into"
              + " stores(id,owner_user_id,commission_policy_id,name,slug,address,phone,open_time,close_time,status,accepting_orders,created_at,updated_at,version)"
              + " values(?,2,1,?,?,"
              + " 'Address','0900000000','00:00:00','23:59:59','ACTIVE',true,?,?,0)",
          id,
          "Shop " + id,
          "shop-" + id,
          time.instant(),
          time.instant());
    jdbc.update(
        "insert into"
            + " store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at)"
            + " values(3,1,'MANAGER',true,4,?,?)",
        time.instant(),
        time.instant());
    jdbc.update(
        "insert into categories(id,name,slug,display_order,is_active,created_at,updated_at)"
            + " values(1,'Tea','tea',1,true,?,?)",
        time.instant(),
        time.instant());
    for (int id = 1; id <= 2; id++) {
      jdbc.update(
          "insert into"
              + " products(id,store_id,category_id,name,slug,base_price,is_active,is_available,is_featured,created_at,updated_at,version)"
              + " values(?,?,1,?,?,30000,true,true,false,?,?,0)",
          id,
          id,
          "Milk tea " + id,
          "tea-" + id,
          time.instant(),
          time.instant());
      jdbc.update(
          "insert into"
              + " product_variants(id,product_id,size_name,extra_price,is_active,created_at,updated_at)"
              + " values(?,?,'M',5000,true,?,?)",
          id,
          id,
          time.instant(),
          time.instant());
    }
    jdbc.update(
        "insert into toppings(id,name,price,is_active,created_at,updated_at)"
            + " values(1,'Pearls',5000,true,?,?)",
        time.instant(),
        time.instant());
    jdbc.update("insert into product_toppings(product_id,topping_id) values(1,1)");
    jdbc.update(
        "insert into store_toppings(store_id,topping_id,is_available,created_at,updated_at)"
            + " values(1,1,true,?,?)",
        time.instant(),
        time.instant());
    jdbc.update(
        "insert into"
            + " user_addresses(id,user_id,recipient_name,phone,address_detail,is_default,created_at,updated_at)"
            + " values(1,1,'Buyer','0900000000','Delivery address',true,?,?)",
        time.instant(),
        time.instant());
    jdbc.update(
        "insert into"
            + " user_addresses(id,user_id,recipient_name,phone,address_detail,is_default,created_at,updated_at)"
            + " values(2,6,'Other','0900000001','Other address',true,?,?)",
        time.instant(),
        time.instant());
    jdbc.update(
        "insert into"
            + " store_delivery_areas(id,store_id,area_code,name,is_active,created_at,updated_at)"
            + " values(1,1,'A','Area A',true,?,?)",
        time.instant(),
        time.instant());
    jdbc.update(
        "insert into"
            + " store_delivery_areas(id,store_id,area_code,name,is_active,created_at,updated_at)"
            + " values(2,2,'B','Area B',true,?,?)",
        time.instant(),
        time.instant());
    jdbc.update(
        "insert into carriers(id,name,base_fee,is_active,created_at,updated_at,version)"
            + " values(1,'Carrier',10000,true,?,?,0)",
        time.instant(),
        time.instant());
    for (int id = 1; id <= 2; id++)
      jdbc.update(
          "insert into"
              + " carrier_area_rates(carrier_id,delivery_area_id,additional_fee,is_active,created_at,updated_at)"
              + " values(1,?,5000,true,?,?)",
          id,
          time.instant(),
          time.instant());
    jdbc.update(
        "insert into loyalty_accounts(user_id,balance,debt,lifetime_earned,updated_at,version)"
            + " values(1,100,0,100,?,0)",
        time.instant());
  }

  Item item(int quantity) {
    return new Item(1, 50, 50, List.of(1L), quantity, "No straw");
  }

  Checkout request(String key, String hash, int used, Long promotion) {
    return new Checkout(key, 1, 1, 1, promotion, null, used, hash, "");
  }

  Map<String, Object> create(int used) {
    carts.add(BUYER, item(2));
    String key = UUID.randomUUID().toString();
    var initial = request(key, "", used, null);
    String hash = checkout.quote(BUYER, initial).get("hash").toString();
    return checkout.checkout(BUYER, request(key, hash, used, null));
  }

  long id(Map<String, Object> value) {
    return ((Number) value.get("id")).longValue();
  }

  long version(long id) {
    return ((Number) workflow.read(BUYER, id).get("version")).longValue();
  }

  void delivered(long id) {
    for (String state : List.of("CONFIRMED", "PICKED_UP", "DELIVERING", "DELIVERED"))
      workflow.transition(
          VENDOR, id, new Transition(state, version(id), "", state.equals("DELIVERED")));
  }

  private void rememberCookies(Map<String, String> cookies, HttpHeaders headers) {
    for (String value : headers.getOrEmpty(HttpHeaders.SET_COOKIE)) {
      String[] pair = value.split(";", 2)[0].split("=", 2);
      cookies.put(pair[0], pair.length == 2 ? pair[1] : "");
    }
  }

  private HttpHeaders browserHeaders(Map<String, String> cookies) {
    var headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    if (!cookies.isEmpty()) headers.set(HttpHeaders.COOKIE, cookies.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(java.util.stream.Collectors.joining("; ")));
    return headers;
  }

  private ResponseEntity<Map> browserPost(String path, Object body, Map<String, String> cookies) {
    var csrf = http.exchange("/products/csrf", HttpMethod.GET,
        new HttpEntity<>(browserHeaders(cookies)), Map.class);
    assertThat(csrf.getStatusCode()).isEqualTo(HttpStatus.OK);
    rememberCookies(cookies, csrf.getHeaders());
    var headers = browserHeaders(cookies);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(csrf.getBody().get("headerName").toString(), csrf.getBody().get("token").toString());
    var response = http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    rememberCookies(cookies, response.getHeaders());
    return response;
  }

  @Test
  void realLoginCatalogGuestMergeCodAndRefundUseTheSameIdentityLedger() {
    Map<String, String> cookies = new LinkedHashMap<>();
    var preview = browserPost("/products/1/quote", Map.of("variantId", 1, "sugarLevel", 50,
        "iceLevel", 50, "toppingIds", List.of(1), "quantity", 2), cookies);
    assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new BigDecimal(preview.getBody().get("total").toString())).isEqualByComparingTo("80000");
    assertThat(browserPost("/cart/items", item(2), cookies).getStatusCode()).isEqualTo(HttpStatus.OK);
    var login = browserPost("/auth/login",
        Map.of("email", "u1@test.invalid", "password", "Only-test-user-password"), cookies);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cookies).containsKey("UTETRA_JWT");
    assertThat(browserPost("/cart/merge", Map.of(), cookies).getStatusCode()).isEqualTo(HttpStatus.OK);
    String key = UUID.randomUUID().toString();
    var quote = browserPost("/checkout/quote", request(key, "", 2, null), cookies);
    assertThat(quote.getStatusCode()).isEqualTo(HttpStatus.OK);
    var created = browserPost("/checkout", request(key, quote.getBody().get("hash").toString(), 2, null), cookies);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    long order = id(created.getBody());
    assertThat(jdbc.queryForObject("select balance from loyalty_accounts where user_id=1", Integer.class)).isEqualTo(98);
    delivered(order);
    var purchases = http.exchange("/reviews/purchases", HttpMethod.GET,
        new HttpEntity<>(browserHeaders(cookies)), List.class);
    assertThat(purchases.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(purchases.getBody()).hasSize(1);
    var summary = http.exchange("/account/loyalty/data", HttpMethod.GET,
        new HttpEntity<>(browserHeaders(cookies)), Map.class);
    assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Number) summary.getBody().get("balance")).intValue()).isEqualTo(105);
    assertThat(browserPost("/orders/" + order + "/returns", Map.of("reason", "Wrong drink"), cookies)
        .getStatusCode()).isEqualTo(HttpStatus.OK);
    workflow.reviewReturn(VENDOR, order, new Decision(true, true, "Quality problem", version(order)));
    workflow.refundCash(VENDOR, order, new RefundInput("Actual cash refunded by vendor", version(order)));
    var refunded = http.exchange("/orders/" + order + "/data", HttpMethod.GET,
        new HttpEntity<>(browserHeaders(cookies)), Map.class);
    assertThat(refunded.getBody()).containsEntry("status", "REFUNDED");
    assertThat(jdbc.queryForObject("select balance from loyalty_accounts where user_id=1", Integer.class)).isEqualTo(100);
    assertThat(jdbc.queryForList("select type from loyalty_transactions where order_id=?", String.class, order))
        .containsExactlyInAnyOrder("REDEEM", "EARN", "REFUND", "REVERSAL");
  }

  @Test
  void revokedLoginReturnsJson401ForOrderRequestsAndRedirectsOnlyHtmlPages() throws Exception {
    Map<String, String> cookies = new LinkedHashMap<>();
    assertThat(browserPost("/auth/login", Map.of("email", "u1@test.invalid",
        "password", "Only-test-user-password"), cookies).getStatusCode()).isEqualTo(HttpStatus.OK);
    jdbc.update("update auth_sessions set revoked_at=? where user_id=1", time.instant());
    var json = http.exchange("/orders/data", HttpMethod.GET,
        new HttpEntity<>(browserHeaders(cookies)), Map.class);
    assertThat(json.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(json.getBody()).containsEntry("error", "UNAUTHORIZED");
    // Inspect the redirect itself rather than the login HTML followed by RestTemplate.
    try (var browser = java.net.http.HttpClient.newBuilder()
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER).build()) {
      var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(http.getRootUri() + "/orders"))
          .header("Accept", "text/html").header("Cookie", browserHeaders(cookies).getFirst(HttpHeaders.COOKIE))
          .GET().build();
      var page = browser.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
      assertThat(page.statusCode()).isEqualTo(302);
      assertThat(page.headers().firstValue("Location").orElseThrow()).endsWith("/auth/login");
    }
  }

  @Test
  void orderJsonNeverTruncatesDecimalOrCoercesStringIdsAndQuantities() {
    Map<String, String> cookies = new LinkedHashMap<>();
    for (String raw : List.of(
        "{\"variantId\":1.9,\"sugar\":50,\"ice\":50,\"quantity\":1}",
        "{\"variantId\":\"1\",\"sugar\":50,\"ice\":50,\"quantity\":1}",
        "{\"variantId\":1,\"sugar\":50,\"ice\":50,\"quantity\":1.9}")) {
      assertThat(browserPost("/cart/items", raw, cookies).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    assertThat(jdbc.queryForObject("select count(*) from cart_items", Integer.class)).isZero();
  }

  @Test
  void disabledCatalogCategoryCannotBeBoughtThroughAnOldCart() {
    carts.add(BUYER, item(1));
    jdbc.update("update categories set is_active=false where id=1");
    assertThatThrownBy(() -> carts.add(BUYER, item(1))).isInstanceOf(OrderProblem.class);
    assertThatThrownBy(() -> checkout.quote(BUYER, request(UUID.randomUUID().toString(), "", 0, null)))
        .isInstanceOf(OrderProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from orders", Integer.class)).isZero();
  }

  @Test
  void catalogAdminMembershipUsesTheSameScopedPolicyForOrdersAndNotifications() {
    long order = id(create(0));
    jdbc.update("update store_memberships set member_role='CATALOG_ADMIN' where user_id=3");
    assertThat(workflow.read(MANAGER, order)).containsEntry("status", "NEW");
    assertThat(operations.stores(MANAGER)).hasSize(1);
    workflow.transition(MANAGER, order, new Transition("CONFIRMED", version(order), "", false));
    assertThat(operations.notifications(MANAGER, 0)).isNotEmpty();
    assertThatThrownBy(() -> operations.commission(MANAGER, new CommissionInput(2, BigDecimal.TEN)))
        .isInstanceOf(OrderProblem.class);
  }

  @Test
  void cartGroupsOnlyIdenticalOptionsAndEnforcesLimits() {
    carts.add(BUYER, item(2));
    var cart = carts.add(BUYER, item(3));
    assertThat((List<?>) cart.get("items")).hasSize(1);
    assertThat(((Map<?, ?>) ((List<?>) cart.get("items")).getFirst()).get("quantity")).isEqualTo(5);
    assertThatThrownBy(() -> carts.add(BUYER, new Item(1, 49, 50, List.of(), 1, "")))
        .isInstanceOf(OrderProblem.class);
    assertThatThrownBy(() -> carts.add(BUYER, item(20))).isInstanceOf(OrderProblem.class);
    assertThatThrownBy(() -> carts.add(BUYER, new Item(2, 50, 50, List.of(), 1, "")))
        .isInstanceOf(OrderProblem.class);
  }

  @Test
  void guestMergeIsAtomicAndIdempotent() {
    var g = new GuestCartService.Snapshot();
    guests.add(g, item(2));
    assertThat(guests.view(g).get("items")).asList().hasSize(1);
    var first = carts.merge(BUYER, List.copyOf(g.items), g.mergeKey);
    carts.merge(BUYER, List.copyOf(g.items), g.mergeKey);
    assertThat(carts.view(BUYER)).isEqualTo(first);
    assertThatThrownBy(
            () ->
                carts.merge(
                    BUYER,
                    List.of(item(1), new Item(2, 50, 50, List.of(), 1, "")),
                    UUID.randomUUID().toString()))
        .isInstanceOf(OrderProblem.class);
    assertThat(((List<?>) carts.view(BUYER).get("items"))).hasSize(1);
  }

  @Test
  void checkoutSnapshotsPricesAndRejectsChangedQuote() {
    carts.add(BUYER, item(2));
    String key = UUID.randomUUID().toString();
    var q = checkout.quote(BUYER, request(key, "", 2, null));
    jdbc.update("update product_variants set extra_price=7000 where id=1");
    assertThatThrownBy(
            () -> checkout.checkout(BUYER, request(key, q.get("hash").toString(), 2, null)))
        .isInstanceOf(OrderProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isZero();
    assertThat(carts.view(BUYER).get("items")).asList().hasSize(1);
  }

  @Test
  void checkoutReplayReturnsOriginalAndDifferentPayloadConflicts() {
    carts.add(BUYER, item(2));
    String key = UUID.randomUUID().toString();
    String hash = checkout.quote(BUYER, request(key, "", 2, null)).get("hash").toString();
    var req = request(key, hash, 2, null);
    var first = checkout.checkout(BUYER, req);
    assertThat(checkout.checkout(BUYER, req)).isEqualTo(first);
    assertThatThrownBy(
            () ->
                checkout.checkout(
                    BUYER, new Checkout(key, 1, 1, 1, null, null, 2, hash, "different")))
        .isInstanceOf(OrderProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select balance from loyalty_accounts where user_id=1", Integer.class))
        .isEqualTo(98);
  }

  @Test
  void codRequiresEveryTransitionAndCashBeforeDelivery() {
    long order = id(create(0));
    assertThatThrownBy(
            () ->
                workflow.transition(
                    VENDOR, order, new Transition("DELIVERED", version(order), "", true)))
        .isInstanceOf(OrderProblem.class);
    for (String state : List.of("CONFIRMED", "PICKED_UP", "DELIVERING"))
      workflow.transition(VENDOR, order, new Transition(state, version(order), "", false));
    assertThatThrownBy(
            () ->
                workflow.transition(
                    VENDOR, order, new Transition("DELIVERED", version(order), "", false)))
        .isInstanceOf(OrderProblem.class);
    workflow.transition(VENDOR, order, new Transition("DELIVERED", version(order), "", true));
    assertThat(workflow.read(BUYER, order).get("paymentStatus")).isEqualTo("PAID");
    assertThat(jdbc.queryForObject("select count(*) from store_settlements", Long.class))
        .isEqualTo(1);
  }

  @Test
  void returnWindowIsExactlyThreeHoursAndRefundReversesPointsOnce() {
    long order = id(create(2));
    delivered(order);
    time.advance(Duration.ofHours(2).plusMinutes(59));
    workflow.requestReturn(BUYER, order, "Wrong drink");
    workflow.reviewReturn(
        VENDOR, order, new Decision(true, true, "Confirmed damaged drink", version(order)));
    workflow.refundCash(VENDOR, order, new RefundInput("receipt-test-1", version(order)));
    assertThat(workflow.read(BUYER, order).get("status")).isEqualTo("REFUNDED");
    assertThat(
            jdbc.queryForObject(
                "select balance from loyalty_accounts where user_id=1", Integer.class))
        .isEqualTo(100);
    assertThatThrownBy(
            () -> workflow.refundCash(VENDOR, order, new RefundInput("again", version(order))))
        .isInstanceOf(OrderProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from refund_transactions", Long.class))
        .isEqualTo(1);
  }

  @Test
  void returnsAtThreeHoursAreRejected() {
    long order = id(create(0));
    delivered(order);
    time.advance(Duration.ofHours(3));
    assertThatThrownBy(() -> workflow.requestReturn(BUYER, order, "Late request"))
        .isInstanceOf(OrderProblem.class);
    assertThat(workflow.read(BUYER, order).get("status")).isEqualTo("DELIVERED");
  }

  @Test
  void rejectedReturnCannotBeSubmittedAgainAndCancelledAssignmentLosesAccess() {
    long returned = id(create(0));
    delivered(returned);
    workflow.requestReturn(BUYER, returned, "Quality problem");
    workflow.reviewReturn(
        VENDOR, returned, new Decision(false, false, "Reason documented", version(returned)));
    assertThatThrownBy(() -> workflow.requestReturn(BUYER, returned, "Retry"))
        .isInstanceOf(OrderProblem.class)
        .hasMessageContaining("một yêu cầu");
    assertThatThrownBy(() -> workflow.validateReturnEligibility(BUYER, returned))
        .isInstanceOf(OrderProblem.class);
    long cancelled = id(create(0));
    workflow.assign(VENDOR, cancelled, new Assignment(5, version(cancelled)));
    workflow.cancel(BUYER, cancelled, version(cancelled), "Changed my mind");
    assertThat(
            jdbc.queryForObject(
                "select status from delivery_assignments where order_id=?",
                String.class,
                cancelled))
        .isEqualTo("CANCELLED");
    assertThatThrownBy(() -> workflow.read(SHIPPER, cancelled)).isInstanceOf(OrderProblem.class);
    assertThat(workflow.list(SHIPPER, "shipper", null, 0, 20)).isEmpty();
  }

  @Test
  void scopedOperationsAndCurrentAssignmentsBlockOtherUsers() {
    long order = id(create(0));
    assertThatThrownBy(() -> workflow.read(BUYER_B, order)).isInstanceOf(OrderProblem.class);
    assertThatThrownBy(
            () -> operations.commission(MANAGER, new CommissionInput(2, new BigDecimal("12"))))
        .isInstanceOf(OrderProblem.class);
    assertThatThrownBy(
            () ->
                operations.carrier(MANAGER, new CarrierInput(null, "No", BigDecimal.ZERO, true, 0)))
        .isInstanceOf(OrderProblem.class);
    workflow.assign(VENDOR, order, new Assignment(5, version(order)));
    assertThat(workflow.read(SHIPPER, order).get("id")).isEqualTo(order);
    assertThatThrownBy(
            () ->
                workflow.transition(
                    SHIPPER, order, new Transition("CONFIRMED", version(order), "", false)))
        .isInstanceOf(OrderProblem.class);
  }

  @Test
  void cancelRestoresRedeemedPointsAndRetainsCartSnapshot() {
    long order = id(create(2));
    workflow.cancel(BUYER, order, version(order), "Changed my mind");
    assertThat(
            jdbc.queryForObject(
                "select balance from loyalty_accounts where user_id=1", Integer.class))
        .isEqualTo(100);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from order_items where order_id=?", Long.class, order))
        .isEqualTo(1);
    assertThatThrownBy(() -> workflow.cancel(BUYER, order, version(order), "again"))
        .isInstanceOf(OrderProblem.class);
  }

  @Test
  void reportUsesActualRefundDateAndScopedNet() {
    long order = id(create(0));
    delivered(order);
    var today = LocalDate.of(2026, 10, 14);
    var before = operations.revenue(VENDOR, 1L, today, today);
    assertThat(before.get("storeNet")).isEqualTo(new BigDecimal("72000.00"));
    workflow.requestReturn(BUYER, order, "Damaged");
    workflow.reviewReturn(VENDOR, order, new Decision(true, true, "Confirmed", version(order)));
    workflow.refundCash(VENDOR, order, new RefundInput("receipt", version(order)));
    assertThat(operations.revenue(VENDOR, 1L, today, today).get("storeNet"))
        .isEqualTo(new BigDecimal("0.00"));
    assertThat(operations.revenue(VENDOR, 1L, today, today).get("refund"))
        .isEqualTo(new BigDecimal("95000.00"));
  }

  @Test
  void browserPagesUseRealJspAndCsrfRejectsForgedMutations() {
    long order = id(create(0));
    String token = jwt.issue(1L, 1L, "ROLE_USER");
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, "UTETRA_JWT=" + token);
    for (String path :
        List.of("/cart", "/checkout", "/orders", "/orders/" + order, "/notifications")) {
      var response = http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).contains("UTETra").doesNotContain("<sitemesh:write", "<%@");
    }
    headers.setContentType(MediaType.APPLICATION_JSON);
    var forged =
        http.exchange(
            "/orders/" + order + "/cancel",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("status", "CANCELLED", "version", version(order), "reason", "x"), headers),
            String.class);
    assertThat(forged.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void simultaneousCheckoutWithSameKeyCreatesOnlyOneOrder() throws Exception {
    carts.add(BUYER, item(2));
    String key = UUID.randomUUID().toString();
    String hash = checkout.quote(BUYER, request(key, "", 0, null)).get("hash").toString();
    Checkout req = request(key, hash, 0, null);
    var gate = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Long> task =
          () -> {
            gate.await();
            return id(checkout.checkout(BUYER, req));
          };
      var a = executor.submit(task);
      var b = executor.submit(task);
      gate.countDown();
      assertThat(a.get(10, TimeUnit.SECONDS)).isEqualTo(b.get(10, TimeUnit.SECONDS));
    }
    assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isEqualTo(1);
  }

  @Test
  void promotionLastSlotCannotBeOversold() throws Exception {
    jdbc.update(
        "insert into"
            + " promotions(id,store_id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version)"
            + " values(1,1,2,'SHOP','PRODUCT_PERCENT','LAST','Last"
            + " slot',10,100000,0,1,1,?,?,true,'SHOP',?,?,0)",
        time.instant().minusSeconds(60),
        time.instant().plusSeconds(3600),
        time.instant(),
        time.instant());
    carts.add(BUYER, item(1));
    carts.add(BUYER_B, item(1));
    String k1 = UUID.randomUUID().toString(), k2 = UUID.randomUUID().toString();
    Checkout p1 = request(k1, "", 0, 1L);
    Checkout p2 = new Checkout(k2, 2, 1, 1, 1L, null, 0, "", "");
    Checkout r1 = request(k1, checkout.quote(BUYER, p1).get("hash").toString(), 0, 1L);
    Checkout r2 =
        new Checkout(
            k2, 2, 1, 1, 1L, null, 0, checkout.quote(BUYER_B, p2).get("hash").toString(), "");
    CountDownLatch gate = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var a =
          executor.submit(
              () -> {
                gate.await();
                try {
                  checkout.checkout(BUYER, r1);
                  return true;
                } catch (OrderProblem e) {
                  return false;
                }
              });
      var b =
          executor.submit(
              () -> {
                gate.await();
                try {
                  checkout.checkout(BUYER_B, r2);
                  return true;
                } catch (OrderProblem e) {
                  return false;
                }
              });
      gate.countDown();
      assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from coupon_redemptions where status='RESERVED'", Long.class))
        .isEqualTo(1);
  }

  @Test
  void insufficientPointsRollBackTheEntireCheckout() {
    carts.add(BUYER, item(2));
    jdbc.update("update loyalty_accounts set balance=0 where user_id=1");
    String key = UUID.randomUUID().toString();
    String hash = checkout.quote(BUYER, request(key, "", 2, null)).get("hash").toString();
    assertThatThrownBy(() -> checkout.checkout(BUYER, request(key, hash, 2, null)))
        .isInstanceOf(OrderProblem.class);
    assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from order_items", Long.class)).isZero();
    assertThat(carts.view(BUYER).get("items")).asList().hasSize(1);
  }

  @Test
  void reassignmentRevokesPreviousShipperAndLogsFailure() {
    long order = id(create(0));
    workflow.assign(VENDOR, order, new Assignment(5, version(order)));
    workflow.transition(VENDOR, order, new Transition("CONFIRMED", version(order), "", false));
    workflow.transition(SHIPPER, order, new Transition("PICKED_UP", version(order), "", false));
    workflow.deliveryFailure(SHIPPER, order, version(order), "Recipient unavailable");
    assertThat(
            operations
                .shipperReport(SHIPPER, LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14))
                .get("failed"))
        .isEqualTo(1L);
    jdbc.update(
        "insert into"
            + " users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version)"
            + " values(7,5,'other-shipper','s@test.invalid','unused','Other',true,?,?,?,0)",
        time.instant(),
        time.instant(),
        time.instant());
    workflow.assign(VENDOR, order, new Assignment(7, version(order)));
    assertThatThrownBy(() -> workflow.read(SHIPPER, order)).isInstanceOf(OrderProblem.class);
    assertThatThrownBy(
            () -> workflow.deliveryFailure(SHIPPER, order, version(order), "old request"))
        .isInstanceOf(OrderProblem.class);
  }

  @Test
  void commissionUpdatesDoNotRewriteOrdersAndReportsNeverLeakScope() {
    long order = id(create(0));
    operations.commission(MANAGER, new CommissionInput(1, new BigDecimal("15.50")));
    assertThat(
            jdbc.queryForObject(
                "select commission_rate from orders where id=?", BigDecimal.class, order))
        .isEqualByComparingTo("10");
    assertThat(operations.globalCommission(ADMIN, new BigDecimal("12"))).isEqualTo(2);
    assertThatThrownBy(() -> operations.globalCommission(MANAGER, new BigDecimal("10")))
        .isInstanceOf(OrderProblem.class);
    assertThatThrownBy(
            () ->
                operations.revenue(
                    MANAGER, 2L, LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14)))
        .isInstanceOf(OrderProblem.class);
  }

  @Test
  void carrierRatesRequireStoreScopeAndNotificationsRequireOwner() {
    assertThat(operations.deliveryAreas(MANAGER))
        .allMatch(a -> ((Number) a.get("store_id")).longValue() == 1);
    assertThat(operations.deliveryRates(MANAGER))
        .allMatch(r -> ((Number) r.get("store_id")).longValue() == 1);
    assertThatThrownBy(() -> operations.deliveryAreas(VENDOR)).isInstanceOf(OrderProblem.class);
    operations.rate(MANAGER, new RateInput(1, 1, new BigDecimal("6000"), true));
    assertThatThrownBy(
            () -> operations.rate(MANAGER, new RateInput(1, 2, new BigDecimal("1"), true)))
        .isInstanceOf(OrderProblem.class);
    long order = id(create(0));
    var notification = operations.notifications(BUYER, 0).getFirst();
    long n = ((Number) notification.get("id")).longValue();
    assertThatThrownBy(() -> operations.readNotification(BUYER_B, n))
        .isInstanceOf(OrderProblem.class);
    operations.readNotification(BUYER, n);
    assertThat(operations.notifications(BUYER, 0).getFirst().get("read")).isEqualTo(true);
  }

  @Test
  void rejectsReturnWithoutEvidenceOrPhysicalAcceptanceAndTracksPointDebt() {
    long order = id(create(0));
    delivered(order);
    jdbc.update("update loyalty_accounts set balance=0 where user_id=1");
    workflow.requestReturn(BUYER, order, "Damaged");
    workflow.reviewReturn(
        MANAGER, order, new Decision(true, false, "Return first", version(order)));
    assertThatThrownBy(
            () -> workflow.refundCash(VENDOR, order, new RefundInput("receipt", version(order))))
        .isInstanceOf(OrderProblem.class);
    workflow.receiveReturn(VENDOR, order, version(order));
    assertThatThrownBy(
            () -> workflow.refundCash(VENDOR, order, new RefundInput("", version(order))))
        .isInstanceOf(OrderProblem.class);
    workflow.refundCash(VENDOR, order, new RefundInput("confirmed-cash", version(order)));
    assertThat(
            jdbc.queryForObject("select debt from loyalty_accounts where user_id=1", Integer.class))
        .isEqualTo(8);
  }

  @Test
  void operatorPagesRenderAndExposeOnlyTheirStore() {
    long order = id(create(0));
    workflow.assign(VENDOR, order, new Assignment(5, version(order)));
    for (Actor actor : List.of(VENDOR, MANAGER, ADMIN, SHIPPER)) {
      String token = jwt.issue(actor.userId(), actor.userId(), actor.role());
      HttpHeaders h = new HttpHeaders();
      h.add(HttpHeaders.COOKIE, "UTETRA_JWT=" + token);
      List<String> urls =
          actor.is("VENDOR")
              ? List.of("/vendor/orders", "/vendor/orders/" + order, "/vendor/revenue")
              : actor.is("MANAGER")
                  ? List.of("/manager/orders", "/manager/order-tools", "/manager/reports")
                  : actor.is("ADMIN")
                      ? List.of("/admin/order-tools", "/admin/reports")
                      : List.of("/shipper/orders", "/shipper/reports");
      for (String path : urls) {
        var response = http.exchange(path, HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UTETra").doesNotContain("<sitemesh:write", "<%@");
      }
    }
  }

  @Test
  void shipperCanReadCompletedAssignmentButCannotChangeDeliveredOrder() {
    long order = id(create(0));
    workflow.assign(VENDOR, order, new Assignment(5, version(order)));
    workflow.transition(VENDOR, order, new Transition("CONFIRMED", version(order), "", false));
    for (String state : List.of("PICKED_UP", "DELIVERING", "DELIVERED"))
      workflow.transition(
          SHIPPER, order, new Transition(state, version(order), "", state.equals("DELIVERED")));
    assertThat(workflow.read(SHIPPER, order).get("status")).isEqualTo("DELIVERED");
    assertThatThrownBy(() -> workflow.deliveryFailure(SHIPPER, order, version(order), "late"))
        .isInstanceOf(OrderProblem.class);
    assertThat(operations.notifications(SHIPPER, 0)).isNotEmpty();
  }

  @Test
  void firstRenderedPageIssuesCsrfCookieBeforeDecoration() {
    var response = http.getForEntity("/cart", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE))
        .anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN="));
  }

  @Test
  void authenticatedPageCsrfSurvivesAssetRequestsAndAuthorizesOnlyMatchingMutations() {
    carts.add(BUYER, item(1));
    HttpHeaders headers = new HttpHeaders();
    String authCookie = "UTETRA_JWT=" + jwt.issue(1L, 1L, "ROLE_USER");
    headers.add(HttpHeaders.COOKIE, authCookie);
    var page = http.exchange("/checkout", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(page.getBody()).doesNotContain(";jsessionid=");
    String csrfCookie =
        page.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
            .filter(value -> value.startsWith("XSRF-TOKEN="))
            .findFirst()
            .orElseThrow()
            .split(";", 2)[0];
    var matcher =
        java.util.regex.Pattern.compile("name=\"_csrf\" content=\"([^\"]+)\"")
            .matcher(page.getBody());
    assertThat(matcher.find()).isTrue();
    String maskedToken = matcher.group(1);
    headers.set(HttpHeaders.COOKIE, authCookie + "; " + csrfCookie);
    for (String asset :
        List.of("/assets/js/orders.js", "/assets/js/utetra.js", "/assets/css/utetra.css")) {
      var response = http.exchange(asset, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE))
          .noneMatch(value -> value.startsWith("XSRF-TOKEN="));
    }
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-XSRF-TOKEN", maskedToken);
    var quoteRequest = request(UUID.randomUUID().toString(), "", 0, null);
    var valid =
        http.exchange(
            "/checkout/quote",
            HttpMethod.POST,
            new HttpEntity<>(quoteRequest, headers),
            String.class);
    assertThat(valid.getStatusCode()).isEqualTo(HttpStatus.OK);
    headers.set("X-XSRF-TOKEN", "forged");
    var invalid =
        http.exchange(
            "/checkout/quote",
            HttpMethod.POST,
            new HttpEntity<>(quoteRequest, headers),
            String.class);
    assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void promotionFundingIsSnapshottedAndCannotChangeOldOrders() {
    jdbc.update(
        "insert into"
            + " promotions(id,store_id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version)"
            + " values(1,1,2,'SHOP','PRODUCT_PERCENT','SNAP','Original',10,100000,0,10,10,?,?,true,'SHOP',?,?,0)",
        time.instant().minusSeconds(60),
        time.instant().plusSeconds(3600),
        time.instant(),
        time.instant());
    carts.add(BUYER, item(1));
    String key = UUID.randomUUID().toString();
    String hash = checkout.quote(BUYER, request(key, "", 0, 1L)).get("hash").toString();
    long order = id(checkout.checkout(BUYER, request(key, hash, 0, 1L)));
    jdbc.update("update promotions set name='Changed',funded_by='APP' where id=1");
    assertThat(
            jdbc.queryForObject(
                "select funded_by_snapshot from coupon_redemptions where order_id=?",
                String.class,
                order))
        .isEqualTo("SHOP");
    assertThat(
            jdbc.queryForObject(
                "select promotion_name_snapshot from coupon_redemptions where order_id=?",
                String.class,
                order))
        .isEqualTo("Original");
  }
}
