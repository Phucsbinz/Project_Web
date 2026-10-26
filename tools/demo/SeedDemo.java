import com.utetra.UtetraApplication;
import com.utetra.identity.*;
import com.utetra.security.*;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicit, additive demo seed. Never deletes data, resets existing accounts or sends email. */
class SeedDemo {
  static JdbcTemplate db;
  static final Instant NOW=Instant.now();
  static long id(String sql,Object... args){return db.queryForObject(sql,Long.class,args);}
  static long insert(String table,String cols,Object... values){
    var key=new GeneratedKeyHolder();
    String sql="insert into "+table+"("+cols+") values("+String.join(",",Collections.nCopies(values.length,"?"))+")";
    db.update(c->{var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<values.length;i++)s.setObject(i+1,values[i] instanceof Instant t?Timestamp.from(t):values[i]);return s;},key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }
  static long existing(String table,String column,String value){var x=db.queryForList("select id from "+table+" where "+column+"=?",Long.class,value);return x.isEmpty()?0:x.getFirst();}
  static long user(String handle,String name,String role,String hash,int index){
    String email=handle+"@example.test";long found=existing("users","email",email);if(found>0)return found;
    return insert("users","role_id,username,email,password_hash,full_name,phone,is_active,email_verified_at,created_at,updated_at,version",id("select id from roles where name=?",role),handle,email,hash,name,"090800"+String.format("%04d",index),true,NOW,NOW,NOW,0);
  }
  static long version(long order){return id("select version from orders where id=?",order);}
  public static void main(String[] args)throws Exception{
    if(!List.of(args).contains("--apply-utetra-demo"))throw new IllegalArgumentException("Explicit --apply-utetra-demo required");
    var menu=new ObjectMapper().readTree(Files.readString(Path.of("tools/demo/menu.json")));
    if(menu.size()!=30)throw new IllegalStateException("Expected 30 drinks");
    try(var ctx=new SpringApplicationBuilder(UtetraApplication.class).run("--spring.profiles.active=mysql","--server.port=0","--server.address=127.0.0.1","--app.mail.enabled=false","--spring.main.banner-mode=off")){
      db=ctx.getBean(JdbcTemplate.class);
      if(!"utetra_db".equals(db.queryForObject("select database()",String.class)))throw new IllegalStateException("Wrong database");
      var tx=new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
      String password="123456",hash=ctx.getBean(PasswordEncoder.class).encode(password);
      long[] admin={0},manager={0},vendor={0};long[] buyers=new long[8],stores=new long[3],areas=new long[3],carrier={0};
      String[] names={"Phan Tuấn Thanh","Nguyễn Minh Anh","Trần Ngọc Linh","Lê Hoàng Nam","Phạm Thu Hà","Võ Gia Huy","Đặng Khánh Vy","Bùi Quang Hưng"};
      tx.executeWithoutResult(status->{
        admin[0]=user("demo.admin","Nguyễn Đức Minh","ROLE_ADMIN",hash,1);
        manager[0]=user("demo.manager","Trần Hoài An","ROLE_MANAGER",hash,2);
        vendor[0]=user("demo.vendor","Lê Phương Thảo","ROLE_VENDOR",hash,3);
        user("demo.shipper","Nguyễn Quốc Bảo","ROLE_SHIPPER",hash,4);
        for(int i=0;i<buyers.length;i++)buyers[i]=user("demo."+List.of("thanh","minhanh","ngoclinh","hoangnam","thuha","giahuy","khanhvy","quanghung").get(i),names[i],"ROLE_USER",hash,10+i);
        long policy=existing("commission_policies","name","UTETra Demo · chiết khấu 5%");
        if(policy==0)policy=insert("commission_policies","name,rate_percent,is_active,created_at,updated_at,version","UTETra Demo · chiết khấu 5%",5,true,NOW,NOW,0);
        carrier[0]=existing("carriers","name","UTETra Demo Delivery");
        if(carrier[0]==0)carrier[0]=insert("carriers","name,base_fee,is_active,created_at,updated_at,version","UTETra Demo Delivery",12000,true,NOW,NOW,0);
        String[] branch={"Thủ Đức","Bình Thạnh","Quận 1"};
        String[] slug={"demo-thu-duc","demo-binh-thanh","demo-quan-1"};
        String[] address={"Khu vực Võ Văn Ngân, TP. Thủ Đức, TP.HCM (chi nhánh mô phỏng)","Khu vực Xô Viết Nghệ Tĩnh, Bình Thạnh, TP.HCM (chi nhánh mô phỏng)","Khu vực Nguyễn Huệ, Quận 1, TP.HCM (chi nhánh mô phỏng)"};
        for(int i=0;i<3;i++){
          stores[i]=existing("stores","slug",slug[i]);
          if(stores[i]==0)stores[i]=insert("stores","owner_user_id,commission_policy_id,name,slug,address,phone,description,image_url,banner_url,open_time,close_time,status,accepting_orders,created_at,updated_at,version",vendor[0],policy,"UTETra "+branch[i],slug[i],address[i],"0908000003","Chi nhánh demo UTETra: trà thơm, cà phê và những cuộc hẹn. Không phải địa điểm kinh doanh thật.","/assets/images/utetra-tea-ritual-branded.png","/assets/images/utetra-tea-ritual-branded.png",Time.valueOf("00:00:00"),Time.valueOf("23:59:59"),"ACTIVE",true,NOW,NOW,0);
          if(id("select count(*) from store_memberships where user_id=? and store_id=?",manager[0],stores[i])==0)insert("store_memberships","user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at",manager[0],stores[i],"MANAGER",true,admin[0],NOW,NOW);
          var found=db.queryForList("select id from store_delivery_areas where store_id=? and area_code=?",Long.class,stores[i],"DEMO_LOCAL");
          areas[i]=found.isEmpty()?insert("store_delivery_areas","store_id,area_code,name,is_active,created_at,updated_at",stores[i],"DEMO_LOCAL","Nội khu "+branch[i],true,NOW,NOW):found.getFirst();
          if(id("select count(*) from carrier_area_rates where carrier_id=? and delivery_area_id=?",carrier[0],areas[i])==0)insert("carrier_area_rates","carrier_id,delivery_area_id,additional_fee,is_active,created_at,updated_at",carrier[0],areas[i],3000,true,NOW,NOW);
        }
        for(int i=0;i<buyers.length;i++)if(id("select count(*) from user_addresses where user_id=?",buyers[i])==0)insert("user_addresses","user_id,recipient_name,phone,address_detail,is_default,created_at,updated_at",buyers[i],names[i],"090800"+String.format("%04d",10+i),"Địa chỉ nhận hàng demo tại TP.HCM — không giao hàng thực tế",true,NOW,NOW);
        long[] tops=new long[3];String[] tn={"Trân châu đen","Trân châu trắng","Thạch vải"};
        for(int i=0;i<3;i++){tops[i]=existing("toppings","name",tn[i]);if(tops[i]==0)tops[i]=insert("toppings","name,price,is_active,created_at,updated_at",tn[i],5000+i*1000,true,NOW,NOW);for(long st:stores)if(id("select count(*) from store_toppings where store_id=? and topping_id=?",st,tops[i])==0)insert("store_toppings","store_id,topping_id,is_available,created_at,updated_at",st,tops[i],true,NOW,NOW);}
        int n=0;
        for(var item:menu){
          String category=item.get("category").asText(),catSlug="demo-category-"+List.of("Trà sữa","Trà trái cây","Matcha & trà rang","Cà phê","Trà nguyên lá","Đá xay & chocolate").indexOf(category);
          long cat=existing("categories","slug",catSlug);
          if(cat==0)cat=insert("categories","name,slug,description,image_url,display_order,is_active,created_at,updated_at",category,catSlug,"Danh mục đồ uống UTETra — dữ liệu demo",item.get("image").asText(),n,true,NOW,NOW);
          for(int st=0;st<3;st++){
            String ps="demo-"+item.get("slug").asText()+"-"+st;
            long product=existing("products","slug",ps);
            if(product==0)product=insert("products","store_id,category_id,name,slug,catalog_key,description,base_price,image_url,is_active,is_available,is_featured,created_at,updated_at,version",stores[st],cat,item.get("name").asText(),ps,"demo-"+item.get("slug").asText(),item.get("description").asText(),item.get("price").asInt(),item.get("image").asText(),true,true,n%4==0,NOW,NOW,0);
            for(int z=0;z<3;z++)if(id("select count(*) from product_variants where product_id=? and size_name=?",product,List.of("S","M","L").get(z))==0)insert("product_variants","product_id,size_name,extra_price,is_active,stock_quantity,version,created_at,updated_at",product,List.of("S","M","L").get(z),z*5000,true,200,0,NOW,NOW);
            for(long top:tops)if(id("select count(*) from product_toppings where product_id=? and topping_id=?",product,top)==0)insert("product_toppings","product_id,topping_id",product,top);
          }n++;
        }
      });
      var carts=ctx.getBean(CartService.class);var checkout=ctx.getBean(CheckoutService.class);var flow=ctx.getBean(OrderWorkflowService.class);var community=ctx.getBean(CommunityService.class);
      var auth=ctx.getBean(AuthService.class);var jwt=ctx.getBean(JwtService.class);
      var principals=new UtetraPrincipal[buyers.length];
      for(int i=0;i<buyers.length;i++)principals[i]=jwt.verify(auth.login(db.queryForObject("select email from users where id=?",String.class,buyers[i]),password).access());
      var operator=new Actor(admin[0],"ROLE_ADMIN");
      int created=0;
      for(int p=0;p<90;p++){
        int st=p%3,mi=p/3,bi=(mi+st)%buyers.length;
        String productSlug="demo-"+menu.get(mi).get("slug").asText()+"-"+st;
        long product=existing("products","slug",productSlug);
        var buyer=new Actor(buyers[bi],"ROLE_USER");
        String key=UUID.nameUUIDFromBytes(("UTETRA-DEMO-V1-"+p).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        var prior=db.queryForList("select id from orders where user_id=? and checkout_key=?",Long.class,buyer.userId(),key);
        long order;
        if(prior.isEmpty()){
          carts.clear(buyer);
          long variant=id("select id from product_variants where product_id=? and size_name=?",product,List.of("S","M","L").get(mi%3));
          carts.add(buyer,new Item(variant,List.of(0,30,50,70,100).get(mi%5),50,List.of(),1+mi%3,"Đơn demo UTETra — không giao hàng thực tế"));
          long address=id("select id from user_addresses where user_id=? order by id limit 1",buyer.userId());
          var request=new Checkout(key,address,areas[st],carrier[0],null,null,0,"pending","Đơn dữ liệu mẫu", "COD");
          String quote=checkout.quote(buyer,request).get("hash").toString();
          var result=checkout.checkout(buyer,new Checkout(key,address,areas[st],carrier[0],null,null,0,quote,"Đơn dữ liệu mẫu", "COD"));
          order=((Number)result.get("id")).longValue();created++;
        } else order=prior.getFirst();
        var states=List.of("NEW","CONFIRMED","PICKED_UP","DELIVERING","DELIVERED");
        int current=states.indexOf(db.queryForObject("select status from orders where id=?",String.class,order));
        if(current<0)throw new IllegalStateException("Demo order changed outside expected workflow");
        for(int step=current+1;step<states.size();step++)flow.transition(operator,order,new Transition(states.get(step),version(order),"Kịch bản demo hợp lệ",states.get(step).equals("DELIVERED")));
        long orderItem=id("select id from order_items where order_id=? order by id limit 1",order);
        if(id("select count(*) from reviews where order_item_id=?",orderItem)==0)community.review(principals[bi],new IdentityForms.Review(product,orderItem,mi%5==0?4:5,"Hương vị cân bằng, món được đóng gói cẩn thận. Trải nghiệm đơn hàng mẫu UTETra."),List.of());
        if(id("select count(*) from product_comments where order_item_id=?",orderItem)==0)community.comment(principals[bi],new IdentityForms.Comment(product,orderItem,"Mình chọn "+menu.get(mi).get("name").asText()+" và thấy vị trà thơm, độ ngọt vừa phải. Đóng gói chắc chắn, món đến đủ theo lựa chọn size và đá. Đây là trải nghiệm mô phỏng của tài khoản demo."),List.of());
        community.favorite(principals[bi],product,true);community.viewed(principals[bi],product);
      }
      // Additional active/cancelled states are created through the same checkout/workflow guards.
      for(int n=0;n<7;n++){
        var buyer=new Actor(buyers[n],"ROLE_USER");int st=n%3;
        String key=UUID.nameUUIDFromBytes(("UTETRA-DEMO-STATE-"+n).getBytes()).toString();
        if(id("select count(*) from orders where user_id=? and checkout_key=?",buyer.userId(),key)>0)continue;
        carts.clear(buyer);long product=existing("products","slug","demo-"+menu.get(n).get("slug").asText()+"-"+st);
        carts.add(buyer,new Item(id("select id from product_variants where product_id=? and size_name='M'",product),50,50,List.of(),2,"Đơn kiểm thử trạng thái"));
        long address=id("select id from user_addresses where user_id=? order by id limit 1",buyer.userId());
        String method=(n==4||n==6)?"BANK_TRANSFER":"COD";
        var q=new Checkout(key,address,areas[st],carrier[0],null,null,0,"pending","Đơn demo trạng thái",method);
        var order=checkout.checkout(buyer,new Checkout(key,address,areas[st],carrier[0],null,null,0,checkout.quote(buyer,q).get("hash").toString(),"Đơn demo trạng thái",method));long oid=((Number)order.get("id")).longValue();
        if(n==5)flow.cancel(buyer,oid,version(oid),"Khách demo yêu cầu hủy trước chuẩn bị");
        else if(n==6)ctx.getBean(BankTransferService.class).confirm(operator,oid,new BankConfirmation("DEMO-UTETRA-"+oid,db.queryForObject("select final_amount from orders where id=?",java.math.BigDecimal.class,oid),version(oid)));
        else if(n<4)for(int step=0;step<n%4;step++)flow.transition(operator,oid,new Transition(List.of("CONFIRMED","PICKED_UP","DELIVERING").get(step),version(oid),"Đơn demo đang xử lý",false));
      }
      for(var p:principals)auth.logout(p,null);
      System.out.println("DEMO_SEED_SUCCESS newDeliveredOrders="+created);
      for(String table:List.of("users","stores","categories","products","product_variants","orders","reviews","product_comments","loyalty_transactions"))System.out.println("COUNT "+table+"="+id("select count(*) from "+table));
    }
  }
}
