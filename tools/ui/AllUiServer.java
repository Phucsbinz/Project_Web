// Isolated H2 UI fixture only. Not packaged in the application WAR.
package com.utetra.order;
import com.utetra.UtetraApplication;
import com.utetra.order.service.*;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.file.*;
public class AllUiServer {
 public static void main(String[] args) throws Exception {
  var app=new SpringApplication(UtetraApplication.class,OrderCommerceIntegrationTest.TimeConfiguration.class,ClockControl.class);
  var context=app.run("--spring.profiles.active=test","--spring.datasource.url=jdbc:h2:mem:utetra_ui_full;MODE=MySQL;DB_CLOSE_DELAY=-1","--payment.bank.enabled=true","--payment.local-qr.base-url=http://127.0.0.1:8882","--server.port=8882","--server.address=127.0.0.1","--spring.main.banner-mode=off","--features.shipper.enabled=true","--payment.vnpay.enabled=true","--payment.vnpay.tmn-code=TESTCODE","--payment.vnpay.hash-secret=test-only-merchant-secret-1234567890","--payment.vnpay.return-url=https://merchant.example.invalid/payments/vnpay/return","--payment.vnpay.ipn-url=https://merchant.example.invalid/payments/vnpay/ipn");
  var test=new OrderCommerceIntegrationTest();
  test.jdbc=context.getBean(JdbcTemplate.class);test.passwords=context.getBean(PasswordEncoder.class);
  test.time=context.getBean(OrderCommerceIntegrationTest.MutableClock.class);
  test.carts=context.getBean(CartService.class);test.checkout=context.getBean(CheckoutService.class);
  test.workflow=context.getBean(OrderWorkflowService.class);
  test.fixture();
  test.jdbc.update("update products set image_url='/assets/images/menu/milk-tea.png'");
  test.jdbc.update("update store_memberships set member_role='CATALOG_ADMIN' where user_id=3");
  test.jdbc.update("update product_variants set stock_quantity=50 where id=1");
  test.jdbc.update("insert into promotions(id,store_id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version) values(1,1,2,'SHOP','PRODUCT_PERCENT','REALTIME_LAST','Last voucher - H2 fixture',10,100000,0,1,1,?,?,true,'SHOP',?,?,0)",test.time.instant().minusSeconds(60),test.time.instant().plusSeconds(3600),test.time.instant(),test.time.instant());
  if("1".equals(System.getenv("UTETRA_VOUCHER_UI")))for(int id=90;id<=92;id++)test.jdbc.update("insert into promotions(id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version) values(?,4,'GLOBAL',?,?,?,10,15000,?,100,100,?,?,?,'APP',?,?,0)",id,id==92?"SHIPPING_PERCENT":"PRODUCT_PERCENT","CODE"+id,id==90?"Đơn từ 500k":id==91?"Ưu đãi tạm ngừng":"Giảm phí giao 10%",id==90?500000:0,test.time.instant().minusSeconds(60),test.time.instant().plusSeconds(3600),id!=91,test.time.instant(),test.time.instant());

  long id=test.id(test.createBank());
  test.carts.add(OrderCommerceIntegrationTest.BUYER,test.item(1));
  Files.writeString(Path.of(".local/all-ui-server.json"),"{\"pid\":"+ProcessHandle.current().pid()+",\"orderId\":"+id+",\"port\":8882}");
  System.out.println("ALL_UI_READY order="+id);
 }
 @org.springframework.web.bind.annotation.RestController
 @org.springframework.context.annotation.Profile("test")
 public static class ClockControl {
  private final JdbcTemplate jdbc;private final OrderCommerceIntegrationTest.MutableClock clock;private final GroupOrderService groups;private final CartService carts;
  public ClockControl(JdbcTemplate jdbc,OrderCommerceIntegrationTest.MutableClock clock,GroupOrderService groups,CartService carts){this.jdbc=jdbc;this.clock=clock;this.groups=groups;this.carts=carts;}
  @org.springframework.web.bind.annotation.PostMapping("/api/ui-fixture/advance") public Object advance(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String,Long> body){jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection->{if(!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:utetra_ui_full"))throw new IllegalStateException("Clock control requires isolated UI H2 fixture");return null;});long seconds=body.getOrDefault("seconds",0L);if(seconds<1||seconds>172800)throw new IllegalArgumentException();clock.advance(java.time.Duration.ofSeconds(seconds));carts.expireInactive();groups.expireGroups();return java.util.Map.of("now",clock.instant().toString());}
 }

}
