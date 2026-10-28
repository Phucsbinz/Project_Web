import java.nio.file.*;import java.sql.*;import java.util.*;import java.math.*;import java.time.*;
/** Explicit offline catalog import: no Spring startup, migrations, providers, actors or transaction seeds. */
public class CatalogSeed {
 static final Map<String,List<String>> NATURAL=Map.of("toppings",List.of("name"),"products",List.of("slug"),"product_variants",List.of("product_id","size_name"),"product_toppings",List.of("product_id","topping_id"),"store_toppings",List.of("store_id","topping_id"));
 static final Map<String,Set<String>> COLS=Map.of(
  "toppings",Set.of("name","price","is_active"),
  "products",Set.of("store_id","category_id","name","slug","catalog_key","description","base_price","is_active","is_available","is_featured","image_url","image_public_id","version"),
  "product_variants",Set.of("product_id","size_name","extra_price","is_active","stock_quantity","version"),
  "product_toppings",Set.of("product_id","topping_id"),"store_toppings",Set.of("store_id","topping_id","is_available"));
 static String identifier(String s){if(!s.matches("[a-zA-Z_][a-zA-Z0-9_]*"))throw new IllegalArgumentException("Invalid identifier");return "`"+s+"`";}
 static String canonical(Object x){if(x==null)return null;if(x instanceof Boolean b)return b?"1":"0";if(x instanceof Number)return new BigDecimal(x.toString()).stripTrailingZeros().toPlainString();return x.toString();}
 static Map<String,String> snapshot(Map<String,Object> row){var out=new LinkedHashMap<String,String>();row.forEach((k,v)->out.put(k,canonical(v)));return out;}
 static List<Map<String,Object>> rows(Connection c,String sql,Object...args)throws Exception{
  var list=new ArrayList<Map<String,Object>>();try(var p=c.prepareStatement(sql)){int n=1;for(Object a:args)p.setObject(n++,a);try(var r=p.executeQuery()){var md=r.getMetaData();while(r.next()){var row=new LinkedHashMap<String,Object>();for(int i=1;i<=md.getColumnCount();i++)row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT),r.getObject(i));list.add(row);}}}return list;
 }
 static String where(List<String> columns){return String.join(" and ",columns.stream().map(x->identifier(x)+"=?").toList());}
 static int write(Connection c,String sql,Object...args)throws Exception{try(var p=c.prepareStatement(sql)){int n=1;for(Object a:args)p.setObject(n++,a);return p.executeUpdate();}}
 static boolean matches(Map<String,Object> actual,Map<String,Object> wanted){return wanted.entrySet().stream().allMatch(e->Objects.equals(canonical(actual.get(e.getKey())),canonical(e.getValue())));}
 @SuppressWarnings("unchecked") static void validate(Map<String,Object> manifest){
  if(!"utetra_db".equals(manifest.get("database")))throw new IllegalArgumentException("Schema contract mismatch");
  var keys=new HashSet<String>();
  for(var op:(List<Map<String,Object>>)manifest.get("operations")){
   String table=op.get("table").toString();var values=(Map<String,Object>)op.get("values");
   if(!COLS.containsKey(table)||!COLS.get(table).equals(values.keySet())||!NATURAL.get(table).equals(op.get("natural"))||!keys.add(op.get("key").toString()))throw new IllegalArgumentException("Manifest whitelist/key mismatch");
   for(var e:values.entrySet()){
    Object v=e.getValue();if(v instanceof String ref&&ref.startsWith("@")&&!keys.contains(ref.substring(1)))throw new IllegalArgumentException("Forward/unknown reference");
    if(Set.of("price","base_price","extra_price").contains(e.getKey())){var amount=new BigDecimal(v.toString());if(amount.signum()<0||amount.stripTrailingZeros().scale()>0||amount.precision()>12)throw new IllegalArgumentException("Invalid VND amount");}
   }
   if(table.equals("products")&&(!Boolean.FALSE.equals(values.get("is_available"))||values.get("image_url")!=null||values.get("image_public_id")!=null))throw new IllegalArgumentException("Unknown-stock products must remain unavailable with existing placeholder");
   if(table.equals("product_variants")&&(!Set.of("S","M","L").contains(values.get("size_name"))||values.get("stock_quantity")!=null))throw new IllegalArgumentException("Do not invent size/stock");
  }
  var operations=(List<Map<String,Object>>)manifest.get("operations");
  var requirements=(List<Map<String,Object>>)manifest.get("requirements");
  var allowedStores=new HashSet<String>();var allowedCategories=new HashSet<String>();var allowedProducts=new HashSet<String>();var allowedToppings=new HashSet<String>();
  for(var r:requirements){String t=r.get("table").toString();var expected=(Map<String,Object>)r.get("expected");
   if(t.equals("stores")&&"ACTIVE".equals(expected.get("status")))allowedStores.add(canonical(r.get("id")));
   if(t.equals("categories")&&Boolean.TRUE.equals(expected.get("is_active")))allowedCategories.add(canonical(r.get("id")));
   if(t.equals("products"))allowedProducts.add(canonical(r.get("id")));
   if(t.equals("toppings")&&Boolean.TRUE.equals(expected.get("is_active")))allowedToppings.add(canonical(r.get("id")));
  }
  for(var op:operations){String table=op.get("table").toString();var v=(Map<String,Object>)op.get("values");String key=op.get("key").toString();
   if(v.containsKey("store_id")&&!allowedStores.contains(canonical(v.get("store_id"))))throw new IllegalArgumentException("Unapproved store scope");
   if(v.containsKey("name")&&(v.get("name")==null||v.get("name").toString().isBlank()||v.get("name").toString().length()>(table.equals("toppings")?100:150)))throw new IllegalArgumentException("Invalid name");
   if(table.equals("products")){
    if(!allowedCategories.contains(canonical(v.get("category_id")))||!v.get("slug").toString().matches("[a-z0-9]+(?:-[a-z0-9]+)*")||v.get("slug").toString().length()>150||!v.get("catalog_key").toString().matches("[a-z0-9]+(?:-[a-z0-9]+)*")||v.get("description").toString().length()>4000)throw new IllegalArgumentException("Invalid product contract");
    var variants=operations.stream().filter(o->"product_variants".equals(o.get("table"))&&("@"+key).equals(((Map)o.get("values")).get("product_id"))).toList();
    var sizes=new HashSet<String>();for(var variant:variants)sizes.add(((Map)variant.get("values")).get("size_name").toString());
    if(variants.isEmpty()||variants.size()>3||variants.size()!=sizes.size()||variants.stream().noneMatch(o->Boolean.TRUE.equals(((Map)o.get("values")).get("is_active"))))throw new IllegalArgumentException("Product requires unique active sizes");
   }
   if(v.containsKey("product_id")&&!(v.get("product_id") instanceof String)&&!allowedProducts.contains(canonical(v.get("product_id"))))throw new IllegalArgumentException("Unapproved existing product");
   if(v.containsKey("topping_id")&&!(v.get("topping_id") instanceof String)&&!allowedToppings.contains(canonical(v.get("topping_id"))))throw new IllegalArgumentException("Unapproved existing topping");
  }
 }
 @SuppressWarnings("unchecked") public static Map<String,Object> seed(Connection c,Map<String,Object> manifest,boolean apply)throws Exception{
  validate(manifest);
  if(apply)for(String table:List.of("categories","stores","products","toppings")){
   var ids=((List<Map<String,Object>>)manifest.get("requirements")).stream().filter(r->table.equals(r.get("table"))).map(r->((Number)r.get("id")).longValue()).distinct().sorted().toList();
   for(long id:ids)rows(c,"select id from "+identifier(table)+" where id=? for update",id);
  }
  // Preflight all approved existing catalog scopes before inserting anything.
  for(var requirement:(List<Map<String,Object>>)manifest.get("requirements")){
   String table=requirement.get("table").toString();if(!Set.of("stores","categories","toppings","products").contains(table))throw new IllegalArgumentException("Invalid prerequisite");
   var found=rows(c,"select * from "+identifier(table)+" where id=?",requirement.get("id"));
   if(found.size()!=1||!matches(found.getFirst(),(Map<String,Object>)requirement.get("expected")))throw new IllegalStateException("Catalog prerequisite changed: "+table+"#"+requirement.get("id"));
  }
  var refs=new HashMap<String,Long>();var created=new ArrayList<Map<String,Object>>();var conflicts=new ArrayList<Map<String,Object>>();var skipped=new ArrayList<String>();long virtual=-1;
  for(var operation:(List<Map<String,Object>>)manifest.get("operations")){
   String key=operation.get("key").toString(),table=operation.get("table").toString();var values=new LinkedHashMap<String,Object>((Map<String,Object>)operation.get("values"));boolean blocked=false;
   for(var e:values.entrySet())if(e.getValue() instanceof String ref&&ref.startsWith("@")){Long id=refs.get(ref.substring(1));if(id==null){blocked=true;break;}e.setValue(id);}
   if(blocked){skipped.add(key);continue;}
   var natural=NATURAL.get(table);var found=rows(c,"select * from "+identifier(table)+" where "+where(natural),natural.stream().map(values::get).toArray());
   if(found.size()>1){conflicts.add(Map.of("key",key,"reason","ambiguous natural key"));continue;}
   if(found.size()==1){
    if(matches(found.getFirst(),values))refs.put(key,((Number)found.getFirst().get("id")).longValue());
    else conflicts.add(Map.of("key",key,"reason","existing values differ; preserved"));
    continue;
   }
   if(table.equals("products")&&!rows(c,"select id from products where store_id=? and (catalog_key=? or (category_id=? and lower(name)=lower(?)))",values.get("store_id"),values.get("catalog_key"),values.get("category_id"),values.get("name")).isEmpty()){
    conflicts.add(Map.of("key",key,"reason","similar product already exists under another slug; preserved"));continue;
   }
   long id=virtual--;
   if(apply){
    var insert=new LinkedHashMap<>(values);if(!table.equals("product_toppings")){insert.put("created_at",Timestamp.from(Instant.now()));insert.put("updated_at",Timestamp.from(Instant.now()));}
    String sql="insert into "+identifier(table)+" ("+String.join(",",insert.keySet().stream().map(CatalogSeed::identifier).toList())+") values ("+String.join(",",Collections.nCopies(insert.size(),"?"))+")";
    try(var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){int n=1;for(Object v:insert.values())p.setObject(n++,v);p.executeUpdate();try(var generated=p.getGeneratedKeys()){if(!generated.next())throw new IllegalStateException("No generated ID");id=generated.getLong(1);}}
   }
   refs.put(key,id);var entry=new LinkedHashMap<String,Object>();entry.put("key",key);entry.put("table",table);entry.put("id",id);entry.put("snapshot",apply?snapshot(rows(c,"select * from "+identifier(table)+" where id=?",id).getFirst()):snapshot(values));created.add(entry);
  }
  return new LinkedHashMap<>(Map.of("created",created,"conflicts",conflicts,"skippedDependencies",skipped));
 }
 @SuppressWarnings("unchecked") static Map<String,Object> rollback(Connection c,Map<String,Object> ledger,boolean apply)throws Exception{
  if(!"committed".equals(ledger.get("status"))||!"catalog-only-v1".equals(ledger.get("format")))throw new IllegalArgumentException("Not a committed catalog ledger");
  var created=(List<Map<String,Object>>)ledger.get("created");var ids=new HashMap<String,Set<Long>>();
  for(var e:created){String table=e.get("table").toString();if(!COLS.containsKey(table)||((Number)e.get("id")).longValue()<=0)throw new IllegalArgumentException("Invalid ledger");ids.computeIfAbsent(table,k->new HashSet<>()).add(((Number)e.get("id")).longValue());}
  var blocked=new ArrayList<Map<String,Object>>();
  for(var e:created){String table=e.get("table").toString();long id=((Number)e.get("id")).longValue();var found=rows(c,"select * from "+identifier(table)+" where id=? for update",id);
   if(found.size()!=1||!snapshot(found.getFirst()).equals(e.get("snapshot"))){blocked.add(Map.of("table",table,"id",id,"reason","missing or edited row"));continue;}
   try(var keys=c.getMetaData().getExportedKeys(c.getCatalog(),c.getSchema(),c.getMetaData().storesUpperCaseIdentifiers()?table.toUpperCase(Locale.ROOT):table)){
    while(keys.next()){
     if(keys.getShort("KEY_SEQ")!=1)throw new IllegalStateException("Composite FK needs manual review");String child=keys.getString("FKTABLE_NAME"),column=keys.getString("FKCOLUMN_NAME");
     String sql="select id from "+identifier(child)+" where "+identifier(column)+"=?";
     for(var childRow:rows(c,sql,id))if(!ids.getOrDefault(child.toLowerCase(Locale.ROOT),Set.of()).contains(((Number)childRow.get("id")).longValue()))blocked.add(Map.of("table",table,"id",id,"reason","new dependency","dependentTable",child));
    }
   }
  }
  if(apply&&!blocked.isEmpty())throw new IllegalStateException("Rollback blocked by edits/dependencies; no deletion performed");
  if(apply){var reverse=new ArrayList<>(created);Collections.reverse(reverse);for(var e:reverse)if(write(c,"delete from "+identifier(e.get("table").toString())+" where id=?",e.get("id"))!=1)throw new IllegalStateException("Rollback row changed");}
  return Map.of("wouldDelete",created.size(),"blocked",blocked,"deleted",apply?created.size():0);
 }
 @SuppressWarnings("unchecked") static Map<String,Object> reconcile(Connection c,Map<String,Object> ledger)throws Exception{
  if(!"prepared".equals(ledger.get("status"))||!"catalog-only-v1".equals(ledger.get("format"))||!"apply".equals(ledger.get("mode")))throw new IllegalArgumentException("Only a prepared apply ledger can be reconciled");
  var entries=(List<Map<String,Object>>)ledger.get("created");if(entries.isEmpty())throw new IllegalStateException("No created IDs to reconcile; manual review required");
  for(var e:entries){String table=e.get("table").toString();if(!COLS.containsKey(table)||((Number)e.get("id")).longValue()<=0)throw new IllegalArgumentException("Ledger table/ID invalid");var found=rows(c,"select * from "+identifier(table)+" where id=?",e.get("id"));if(found.size()!=1||!snapshot(found.getFirst()).equals(e.get("snapshot")))throw new IllegalStateException("Commit outcome uncertain or row edited; manual review required");}
  var result=new LinkedHashMap<String,Object>(ledger);result.put("status","committed");result.put("reconciledAtUtc",Instant.now().toString());return result;
 }
 static void saveNewReport(Path file,String text)throws Exception{
  try(var channel=java.nio.channels.FileChannel.open(file,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
   var bytes=java.nio.ByteBuffer.wrap(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));while(bytes.hasRemaining())channel.write(bytes);channel.force(true);
  }
 }
 static void replaceReport(Path file,String text)throws Exception{
  Path temporary=file.resolveSibling(file.getFileName()+"."+UUID.randomUUID()+".complete");saveNewReport(temporary,text);
  // Same-directory atomic replacement: a failure leaves the complete prepared ID ledger recoverable.
  Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
 }
 public static void main(String[] args){
  try{
   String mode=args[0];if(!Set.of("preview","apply","rollback-preview","rollback","reconcile").contains(mode))throw new IllegalArgumentException("Mode");var json=new com.fasterxml.jackson.databind.ObjectMapper();Path output=Path.of(args[2]);if(Files.exists(output))throw new IllegalArgumentException("Output exists; use a new ledger filename");
   var config=new Properties();try(var reader=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){config.load(reader);}String url=config.getProperty("spring.datasource.url");if(url==null||!url.matches("jdbc:mysql://[^/]+/utetra_db(?:\\?.*)?"))throw new IllegalArgumentException("Unexpected database");
   try(var c=DriverManager.getConnection(url,config.getProperty("spring.datasource.username"),config.getProperty("spring.datasource.password"))){
    c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);if(!"utetra_db".equals(rows(c,"select database() as db").getFirst().get("db")))throw new IllegalStateException("Schema mismatch");
    try{
     Map<String,Object> result;
     if(mode.equals("reconcile"))result=reconcile(c,json.readValue(Files.readString(Path.of(args[3])),Map.class));
     else if(mode.startsWith("rollback"))result=rollback(c,json.readValue(Files.readString(Path.of(args[3])),Map.class),mode.equals("rollback"));
     else result=seed(c,json.readValue(Files.readString(Path.of(args[1])),Map.class),mode.equals("apply"));
     result=new LinkedHashMap<>(result);result.put("format","catalog-only-v1");result.put("database","utetra_db");result.put("capturedAtUtc",Instant.now().toString());result.put("mode",mode);if(!mode.equals("reconcile"))result.put("status","prepared");
     if(output.getParent()!=null)Files.createDirectories(output.getParent());saveNewReport(output,json.writerWithDefaultPrettyPrinter().writeValueAsString(result));
     if(Set.of("apply","rollback").contains(mode)){c.commit();result.put("status","committed");}else{c.rollback();if(!mode.equals("reconcile"))result.put("status","preview-no-writes");}
     replaceReport(output,json.writerWithDefaultPrettyPrinter().writeValueAsString(result));System.out.println("Catalog "+mode+" complete. Details: "+output);
    }catch(Exception e){c.rollback();throw e;}
   }
  }catch(Exception e){System.err.println("Catalog operation failed: "+e.getClass().getSimpleName()+" (no credentials or connection string printed)");System.exit(1);}
 }
}
