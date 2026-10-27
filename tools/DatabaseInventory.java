import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Read-only counts/schema inventory. No account names, passwords or data rows are exported. */
class DatabaseInventory {
 public static void main(String[] args){
  try{
   var p=new Properties();try(var reader=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){p.load(reader);}
   String url=p.getProperty("spring.datasource.url");if(url==null||!url.matches("jdbc:mysql://[^/]+/utetra_db(?:\\?.*)?"))throw new IllegalStateException("Unexpected database");
   var report=new LinkedHashMap<String,Object>();var tables=new ArrayList<Map<String,Object>>();var views=new ArrayList<String>();
   try(var c=DriverManager.getConnection(url,p.getProperty("spring.datasource.username"),p.getProperty("spring.datasource.password"))){
    c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
    try(var s=c.createStatement();var r=s.executeQuery("select database()")){r.next();if(!"utetra_db".equals(r.getString(1)))throw new IllegalStateException("Unexpected schema");report.put("database",r.getString(1));}
    var names=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("select table_name,table_type from information_schema.tables where table_schema=database() order by table_name")){while(r.next()){if("BASE TABLE".equals(r.getString(2)))names.add(r.getString(1));else views.add(r.getString(1));}}
    for(String name:names){
     if(!name.matches("[a-zA-Z0-9_]+"))throw new IllegalStateException("Unexpected table identifier");
     var table=new LinkedHashMap<String,Object>();table.put("name",name);
     try(var s=c.createStatement();var r=s.executeQuery("select count(*) from `"+name+"`")){r.next();table.put("rows",r.getLong(1));}
     var columns=new ArrayList<Map<String,String>>();
     try(var s=c.prepareStatement("select column_name,column_type,is_nullable,column_key,extra from information_schema.columns where table_schema=database() and table_name=? order by ordinal_position")){s.setString(1,name);try(var r=s.executeQuery()){while(r.next()){var column=new LinkedHashMap<String,String>();column.put("name",r.getString(1));column.put("type",r.getString(2));column.put("nullable",r.getString(3));column.put("key",r.getString(4));column.put("extra",r.getString(5));columns.add(column);}}}
     table.put("columns",columns);
     var foreignKeys=new ArrayList<Map<String,String>>();
     try(var s=c.prepareStatement("select column_name,referenced_table_name,referenced_column_name from information_schema.key_column_usage where table_schema=database() and table_name=? and referenced_table_name is not null order by column_name,referenced_table_name")){s.setString(1,name);try(var r=s.executeQuery()){while(r.next())foreignKeys.add(Map.of("column",r.getString(1),"table",r.getString(2),"target",r.getString(3)));}}
     table.put("foreignKeys",foreignKeys);tables.add(table);
    }
    var migrations=new ArrayList<Map<String,Object>>();try(var s=c.createStatement();var r=s.executeQuery("select version,description,success from flyway_schema_history order by installed_rank")){while(r.next())migrations.add(Map.of("version",r.getString(1),"description",r.getString(2),"success",r.getBoolean(3)));}report.put("migrations",migrations);c.commit();
   }
   report.put("capturedAtUtc",java.time.Instant.now().toString());report.put("physicalTableCount",tables.size());report.put("applicationTableCount",tables.stream().filter(t->!t.get("name").equals("flyway_schema_history")).count());report.put("views",views);report.put("tables",tables);
   String json=new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report);
   Path output=Path.of(args.length>0?args[0]:".local/database-inventory.json");if(output.getParent()!=null)Files.createDirectories(output.getParent());Files.writeString(output,json+"\n");
   System.out.println("Read-only inventory complete: "+tables.size()+" physical tables; report "+output);
  }catch(Exception e){System.err.println("Inventory failed: "+e.getClass().getSimpleName()+"; credentials/data rows were not printed.");System.exit(1);}
 }
}
