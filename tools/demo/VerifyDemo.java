import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Read-only consistency checks against the configured database; never prints credentials. */
class VerifyDemo {
  static Connection c;
  static long scalar(String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
  static void zero(String label,String sql)throws Exception{long n=scalar(sql);System.out.println("CHECK "+label+" violations="+n);if(n!=0)throw new IllegalStateException(label);}
  public static void main(String[] args)throws Exception{
    var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){p.load(r);}
    try(var connection=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
      c=connection; // TiDB does not implement JDBC SET TRANSACTION READ ONLY; every query below is SELECT.
      try(var s=c.createStatement();var r=s.executeQuery("select database()")){r.next();if(!"utetra_db".equals(r.getString(1)))throw new IllegalStateException("Wrong DB");}
      for(String t:List.of("users","stores","categories","products","product_variants","orders","reviews","product_comments","loyalty_transactions"))System.out.println("COUNT "+t+"="+scalar("select count(*) from "+t));
      if(List.of(args).contains("--progress"))return;
      if(scalar("select count(*) from products where slug like 'demo-%'")!=90)throw new IllegalStateException("Expected 30 drinks x 3 branches");
      if(scalar("select count(*) from orders where status='DELIVERED' and note like '%demo%'")<9)throw new IllegalStateException("Missing office demo orders");
      zero("missing_catalog_key","select count(*) from products where slug like 'demo-%' and catalog_key is null");
      if(scalar("select count(distinct catalog_key) from products where slug like 'demo-%'")!=30)throw new IllegalStateException("Expected 30 explicit drink keys");
      zero("missing_images","select count(*) from products where slug like 'demo-%' and (image_url is null or image_url not like '/assets/images/menu/%')");
      zero("missing_sizes","select count(*) from products p where p.slug like 'demo-%' and (select count(*) from product_variants v where v.product_id=p.id and v.is_active=true)<>3");
      zero("order_totals","select count(*) from orders where final_amount<>subtotal-product_discount-points_discount+shipping_gross-shipping_discount or subtotal<0 or final_amount<0");
      zero("commission_totals","select count(*) from orders where commission_amount+store_net<>subtotal-product_discount-points_discount");
      zero("review_purchase_evidence","select count(*) from reviews r left join order_items i on i.id=r.order_item_id left join orders o on o.id=i.order_id left join product_variants v on v.id=i.variant_id where o.id is null or o.user_id<>r.author_id or v.product_id<>r.product_id or o.status<>'DELIVERED' or o.payment_status<>'PAID' or r.stars not between 1 and 5");
      zero("comment_purchase_evidence","select count(*) from product_comments r left join order_items i on i.id=r.order_item_id left join orders o on o.id=i.order_id left join product_variants v on v.id=i.variant_id where o.id is null or o.user_id<>r.author_id or v.product_id<>r.product_id or o.status<>'DELIVERED' or o.payment_status<>'PAID' or char_length(trim(r.content)) not between 50 and 2000");
      zero("negative_stock","select count(*) from product_variants where stock_quantity<0");
      zero("inventory_consistency","select count(*) from product_variants v join products p on p.id=v.product_id where p.slug like 'demo-%' and v.stock_quantity<>200-coalesce((select sum(i.quantity) from order_items i where i.variant_id=v.id and i.stock_reserved=true),0)");
      zero("paid_transaction","select count(*) from orders o where o.payment_status='PAID' and not exists(select 1 from payment_transactions t where t.order_id=o.id and t.status='SUCCEEDED' and t.amount=o.final_amount)");
      zero("missing_delivered_history","select count(*) from orders o join users u on u.id=o.user_id where u.email like 'demo.%@example.test' and o.status='DELIVERED' and (select count(*) from order_status_history h where h.order_id=o.id)<5");
      zero("product_without_review","select count(*) from products p where p.slug like 'demo-%' and not exists(select 1 from reviews r where r.product_id=p.id and r.status='VISIBLE')");
      zero("points_balance","select count(*) from loyalty_accounts a join users u on u.id=a.user_id where u.email like 'demo.%@example.test' and a.balance<>coalesce((select sum(t.points_delta) from loyalty_transactions t where t.account_id=a.id),0)");
      try(var s=c.createStatement();var r=s.executeQuery("select status,payment_method,payment_status,count(*) from orders group by status,payment_method,payment_status order by status")){while(r.next())System.out.println("STATE "+r.getString(1)+" "+r.getString(2)+" "+r.getString(3)+"="+r.getLong(4));}
      System.out.println("DEMO_VALIDATION_OK");
    }
  }
}
