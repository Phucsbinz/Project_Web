import java.nio.file.*;
import java.sql.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
/** Replace only the known generated boilerplate for demo-owned products and demo authors. */
class RepairDemoFeedback {
 public static void main(String[] args)throws Exception {
  boolean apply=List.of(args).contains("--apply");var json=new ObjectMapper();Map<String,JsonNode> drinks=new HashMap<>();
  for(var item:json.readTree(Files.readString(Path.of("tools/demo/menu.json"))))drinks.put("demo-"+item.path("slug").asText(),item);
  if(drinks.size()!=30)throw new IllegalStateException("Expected 30 demo drinks");
  Set<String> oldReviews=Set.of("Hương vị cân bằng, món được đóng gói cẩn thận. Trải nghiệm đơn hàng mẫu UTETra.","Trà thơm rõ vị, mình chọn ít đường nên uống rất vừa. Dữ liệu trải nghiệm demo.","Món được đóng gói gọn, đá vừa phải và vị sữa cân bằng. Đánh giá của tài khoản demo.","Mình thích hậu vị thanh và có thể chọn size theo ý. Phản hồi mô phỏng đơn đã mua.","Hương vị ổn, lần sau mình sẽ chọn ít ngọt hơn. Đây là đánh giá trong dữ liệu demo.");
  var properties=new Properties();try(var reader=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){properties.load(reader);}
  try(var connection=DriverManager.getConnection(properties.getProperty("spring.datasource.url"),properties.getProperty("spring.datasource.username"),properties.getProperty("spring.datasource.password"))){
   if(!"utetra_db".equals(connection.getCatalog()))throw new IllegalStateException("Wrong database");connection.setAutoCommit(false);
   try{
    int changed=0,eligible=0;Set<String> covered=new HashSet<>();
    for(String table:List.of("reviews","product_comments")){
     record Change(long id,String previous,String content){}var changes=new ArrayList<Change>();
     String sql="select f.id,f.content,p.name,p.slug,p.catalog_key,i.size_name,i.sugar_level,i.ice_level from "+table+" f join users a on a.id=f.author_id join products p on p.id=f.product_id join stores s on s.id=p.store_id join users owner on owner.id=s.owner_user_id join order_items i on i.id=f.order_item_id join orders o on o.id=i.order_id where a.email like 'demo.%@example.test' and owner.email='demo.vendor@example.test' and p.catalog_key like 'demo-%' and o.user_id=f.author_id and o.status='DELIVERED' and o.payment_status='PAID' order by f.id";
     try(var statement=connection.createStatement();var rows=statement.executeQuery(sql)){
      while(rows.next()){
       var drink=drinks.get(rows.getString("catalog_key"));if(drink==null)continue;
       String previous=rows.getString("content"),name=rows.getString("name");boolean comment=table.equals("product_comments");
       String oldComment="Mình chọn "+name+" và thấy vị trà thơm, độ ngọt vừa phải. Đóng gói chắc chắn, món đến đủ theo lựa chọn size và đá. Đây là trải nghiệm mô phỏng của tài khoản demo.";

       String slug=rows.getString("slug");int branch=Integer.parseInt(slug.substring(slug.lastIndexOf('-')+1));
       if(branch<0||branch>2)throw new IllegalStateException("Unexpected demo branch");
       String text=FeedbackCopy.render(drink,comment,branch,rows.getString("size_name"),rows.getInt("sugar_level"),rows.getInt("ice_level"));
       boolean old=comment?oldComment.equals(previous):oldReviews.contains(previous);if(!old&&!previous.equals(text+" Nội dung demo."))continue;
       if(drink.path("category").asText().equals("Cà phê")&&text.contains("vị trà thơm"))throw new IllegalStateException("Coffee feedback mentions tea");
       changes.add(new Change(rows.getLong("id"),previous,text));covered.add(rows.getString("catalog_key"));
      }
     }
     eligible+=changes.size();
     if(apply)try(var update=connection.prepareStatement("update "+table+" set content=?,updated_at=current_timestamp(6),version=version+1 where id=? and content=?")){
      for(var row:changes){update.setString(1,row.content());update.setLong(2,row.id());update.setString(3,row.previous());if(update.executeUpdate()!=1)throw new IllegalStateException("Feedback changed concurrently; rolled back");changed++;}
     }
     System.out.println(table+" eligible="+changes.size());
    }
    if(apply)connection.commit();else connection.rollback();System.out.println("FEEDBACK_"+(apply?"UPDATED":"PLAN")+" eligible="+eligible+" changed="+changed+" drinks="+covered.size());
   }catch(Exception failure){connection.rollback();throw failure;}
  }
 }
}
