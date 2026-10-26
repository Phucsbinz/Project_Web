import com.utetra.UtetraApplication;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.*;

/** Demo office group orders: 44 cups/order, below the application's 50-cup guard. */
class ShowcaseOrders extends SeedDemo {
 public static void main(String[] args)throws Exception{
  if(!List.of(args).contains("--apply-utetra-demo"))throw new IllegalArgumentException("Explicit apply required");
  try(var ctx=new SpringApplicationBuilder(UtetraApplication.class).run("--spring.profiles.active=mysql","--server.port=0","--server.address=127.0.0.1","--app.mail.enabled=false")){
   db=ctx.getBean(JdbcTemplate.class);if(!"utetra_db".equals(db.queryForObject("select database()",String.class)))throw new IllegalStateException("Wrong DB");
   var buyer=new Actor(existing("users","email","demo.thanh@example.test"),"ROLE_USER");
   var admin=new Actor(existing("users","email","demo.admin@example.test"),"ROLE_ADMIN");
   var carts=ctx.getBean(CartService.class);var checkout=ctx.getBean(CheckoutService.class);var flow=ctx.getBean(OrderWorkflowService.class);
   var menu=new com.fasterxml.jackson.databind.ObjectMapper().readTree(java.nio.file.Files.readString(java.nio.file.Path.of("tools/demo/menu.json")));
   long address=id("select id from user_addresses where user_id=? order by id limit 1",buyer.userId());
   long carrier=existing("carriers","name","UTETra Demo Delivery");
   for(int st=0;st<3;st++)for(int group=0;group<3;group++){
    String key=UUID.nameUUIDFromBytes(("UTETRA-DEMO-OFFICE-"+st+"-"+group).getBytes()).toString();
    if(id("select count(*) from orders where user_id=? and checkout_key=?",buyer.userId(),key)>0)continue;
    long store=existing("stores","slug",List.of("demo-thu-duc","demo-binh-thanh","demo-quan-1").get(st));
    long area=id("select id from store_delivery_areas where store_id=? and area_code='DEMO_LOCAL'",store);
    carts.clear(buyer);
    for(int p=group*4;p<group*4+4;p++){
     long product=existing("products","slug","demo-"+menu.get(p).get("slug").asText()+"-"+st);
     long variant=id("select id from product_variants where product_id=? and size_name='M'",product);
     carts.add(buyer,new Item(variant,50,50,List.of(),11,"Đặt nhóm văn phòng — đơn demo, không giao thực tế"));
    }
    var q=new Checkout(key,address,area,carrier,null,null,0,"pending","Đơn nhóm 44 ly - demo","COD");
    var result=checkout.checkout(buyer,new Checkout(key,address,area,carrier,null,null,0,checkout.quote(buyer,q).get("hash").toString(),"Đơn nhóm 44 ly - demo","COD"));
    long order=((Number)result.get("id")).longValue();
    for(String state:List.of("CONFIRMED","PICKED_UP","DELIVERING","DELIVERED"))flow.transition(admin,order,new Transition(state,version(order),"Giao đơn nhóm demo",state.equals("DELIVERED")));
   }
   new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
    if(existing("promotions","code","UTETRADEMO10")==0)insert("promotions","created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version",admin.userId(),"GLOBAL","PRODUCT_PERCENT","UTETRADEMO10","Demo: giảm 10% đơn từ 60.000đ",10,20000,60000,1000,5,NOW.minusSeconds(86400),NOW.plusSeconds(30L*86400),true,"APP",NOW,NOW,0);
    if(existing("promotions","code","UTETRADEMOSHIP")==0)insert("promotions","created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version",admin.userId(),"GLOBAL","SHIPPING_PERCENT","UTETRADEMOSHIP","Demo: hỗ trợ phí giao tối đa 15.000đ",100,15000,100000,1000,5,NOW.minusSeconds(86400),NOW.plusSeconds(30L*86400),true,"APP",NOW,NOW,0);
    String[] reviews={"Trà thơm rõ vị, mình chọn ít đường nên uống rất vừa. Dữ liệu trải nghiệm demo.","Món được đóng gói gọn, đá vừa phải và vị sữa cân bằng. Đánh giá của tài khoản demo.","Mình thích hậu vị thanh và có thể chọn size theo ý. Phản hồi mô phỏng đơn đã mua.","Hương vị ổn, lần sau mình sẽ chọn ít ngọt hơn. Đây là đánh giá trong dữ liệu demo."};
    int i=0;for(var row:db.queryForList("select r.id from reviews r join users u on u.id=r.author_id where u.email like 'demo.%@example.test' and r.content='Hương vị cân bằng, món được đóng gói cẩn thận. Trải nghiệm đơn hàng mẫu UTETra.' order by r.id"))db.update("update reviews set content=? where id=?",reviews[i++%reviews.length],row.get("id"));
   });
   System.out.println("SHOWCASE_ORDERS_OK");
  }
 }
}
