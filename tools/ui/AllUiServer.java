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
  var app=new SpringApplication(UtetraApplication.class,OrderCommerceIntegrationTest.TimeConfiguration.class);
  var context=app.run("--spring.profiles.active=test","--spring.datasource.url=jdbc:h2:mem:utetra_ui_full;MODE=MySQL;DB_CLOSE_DELAY=-1","--server.port=8882","--server.address=127.0.0.1","--spring.main.banner-mode=off","--features.shipper.enabled=true","--payment.vnpay.enabled=true","--payment.vnpay.tmn-code=TESTCODE","--payment.vnpay.hash-secret=test-only-merchant-secret-1234567890","--payment.vnpay.return-url=https://merchant.example.invalid/payments/vnpay/return","--payment.vnpay.ipn-url=https://merchant.example.invalid/payments/vnpay/ipn");
  var test=new OrderCommerceIntegrationTest();
  test.jdbc=context.getBean(JdbcTemplate.class);test.passwords=context.getBean(PasswordEncoder.class);
  test.time=context.getBean(OrderCommerceIntegrationTest.MutableClock.class);
  test.carts=context.getBean(CartService.class);test.checkout=context.getBean(CheckoutService.class);
  test.workflow=context.getBean(OrderWorkflowService.class);
  test.fixture();
  test.jdbc.update("update store_memberships set member_role='CATALOG_ADMIN' where user_id=3");
  test.jdbc.update("update product_variants set stock_quantity=50 where id=1");
  test.jdbc.update("insert into promotions(id,store_id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version) values(1,1,2,'SHOP','PRODUCT_PERCENT','REALTIME_LAST','Last voucher - H2 fixture',10,100000,0,1,1,?,?,true,'SHOP',?,?,0)",test.time.instant().minusSeconds(60),test.time.instant().plusSeconds(3600),test.time.instant(),test.time.instant());
  long id=test.id(test.createBank());
  test.carts.add(OrderCommerceIntegrationTest.BUYER,test.item(1));
  Files.writeString(Path.of(".local/all-ui-server.json"),"{\"pid\":"+ProcessHandle.current().pid()+",\"orderId\":"+id+",\"port\":8882}");
  System.out.println("ALL_UI_READY order="+id);
 }
}
