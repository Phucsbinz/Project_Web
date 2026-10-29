import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Only assigns the known seeded menu families; never infers identity from names. */
class AssignCatalogKeys {
 public static void main(String[] args)throws Exception{
  if(!List.of(args).contains("--apply-utetra-demo"))throw new IllegalArgumentException("Explicit apply required");
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){p.load(r);}
  var menu=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of("tools/demo/menu.json")));
  try(var c=DriverManager.getConnection(p.getProperty("spring.datasource.url"),p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
   try(var s=c.createStatement();var r=s.executeQuery("select database()")){r.next();if(!"utetra_db".equals(r.getString(1)))throw new IllegalStateException("Wrong database");}
   c.setAutoCommit(false);int changed=0;
   try{
    for(var item:menu)for(int branch=0;branch<3;branch++){
     String key="demo-"+item.get("slug").asText();
     try(var s=c.prepareStatement("update products p join stores s on s.id=p.store_id join users u on u.id=s.owner_user_id set p.catalog_key=? where p.slug=? and u.email='thaolp.vendor@example.test' and (p.catalog_key is null or p.catalog_key=?)")){
      s.setString(1,key);s.setString(2,key+"-"+branch);s.setString(3,key);changed+=s.executeUpdate();
     }
    }
    if(changed!=90)throw new IllegalStateException("Expected exactly 90 known demo product records, got "+changed);
    c.commit();System.out.println("Assigned 30 catalog keys across 90 demo product records.");
   }catch(Exception ex){c.rollback();throw ex;}
  }
 }
}
