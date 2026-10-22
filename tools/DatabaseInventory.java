import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Read-only inventory of the configured UTETra database. Never prints credentials. */
class DatabaseInventory {
 public static void main(String[] args) throws Exception {
  var p=new Properties();try(var r=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){p.load(r);}
  String url=p.getProperty("spring.datasource.url");
  if(!url.matches("jdbc:mysql://[^/]+/utetra_db(?:\\?.*)?"))throw new IllegalStateException("Unexpected database");
  try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"));var s=c.createStatement()){
   try(var r=s.executeQuery("select database(),version()")){r.next();System.out.println("Database="+r.getString(1)+" Server="+r.getString(2));}
   try(var r=s.executeQuery("select table_name from information_schema.tables where table_schema=database() order by table_name")){while(r.next())System.out.println("TABLE "+r.getString(1));}
   try(var r=s.executeQuery("select version,description,success from flyway_schema_history order by installed_rank")){while(r.next())System.out.println("MIGRATION "+r.getString(1)+" "+r.getString(2)+" "+r.getBoolean(3));}
   try(var r=s.executeQuery("select r.name,count(*),sum(case when u.is_active and u.email_verified_at is not null then 1 else 0 end) from users u join roles r on r.id=u.role_id group by r.name")){while(r.next())System.out.println("ACCOUNTS "+r.getString(1)+" total="+r.getLong(2)+" activeVerified="+r.getLong(3));}
  }catch(SQLException e){System.out.println("Database connection/query failed: SQLState="+e.getSQLState()+" code="+e.getErrorCode());System.exit(1);}
 }
}
