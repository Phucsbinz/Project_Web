import java.nio.file.*;import java.sql.*;import java.util.*;
class CatalogPostCheck {
 public static void main(String[] args){try{var p=new Properties();try(var reader=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){p.load(reader);}var result=new LinkedHashMap<String,Object>();try(var c=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){c.setAutoCommit(false);
  var checks=new LinkedHashMap<String,Object>();
  for(String sql:List.of(
   "select count(*) as failures from products p left join categories c on c.id=p.category_id left join stores s on s.id=p.store_id where c.id is null or s.id is null",
   "select count(*) as failures from product_variants v left join products p on p.id=v.product_id where p.id is null",
   "select count(*) as failures from product_toppings x left join products p on p.id=x.product_id left join toppings t on t.id=x.topping_id where p.id is null or t.id is null",
   "select count(*) as failures from store_toppings x left join stores s on s.id=x.store_id left join toppings t on t.id=x.topping_id where s.id is null or t.id is null",
   "select count(*) as failures from (select product_id,size_name from product_variants group by product_id,size_name having count(*)>1) d",
   "select count(*) as failures from (select product_id,topping_id from product_toppings group by product_id,topping_id having count(*)>1) d",
   "select count(*) as failures from (select store_id,topping_id from store_toppings group by store_id,topping_id having count(*)>1) d",
   "select count(*) as failures from products where catalog_key like 'ref-20261028-%' and (is_available=true or image_url is not null or base_price<0)",
   "select count(*) as failures from product_variants v join products p on p.id=v.product_id where p.catalog_key like 'ref-20261028-%' and (v.stock_quantity is not null or v.extra_price<0 or v.size_name not in ('S','M','L'))"))checks.put("check-"+(checks.size()+1),CatalogSurvey.rows(c,sql).getFirst().get("failures"));
  result.put("checks",checks);result.put("databaseTriggers",CatalogSurvey.rows(c,"select count(*) as count from information_schema.triggers where trigger_schema=database()"));result.put("existingQuoteCandidate",CatalogSurvey.rows(c,"select p.id as product_id,v.id as variant_id,p.base_price,v.extra_price,v.stock_quantity,t.id as topping_id,t.price as topping_price from products p join product_variants v on v.product_id=p.id join product_toppings pt on pt.product_id=p.id join toppings t on t.id=pt.topping_id join store_toppings st on st.store_id=p.store_id and st.topping_id=t.id where p.catalog_key='demo-taro' and p.store_id=1 and p.is_active=true and p.is_available=true and v.is_active=true and v.stock_quantity>0 and t.name='Sương sáo' and st.is_available=true order by v.extra_price,v.id limit 1"));c.rollback();}
  Files.writeString(Path.of("docs/data-seeding/2026-10-28/data-checks.json"),new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));System.out.println("Catalog FK/unique/stock/price checks saved; no transaction writes.");
 }catch(Exception e){System.err.println("Post-check failed: "+e.getClass().getSimpleName());System.exit(1);}}
}
