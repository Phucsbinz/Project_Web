import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import com.cloudinary.Cloudinary;

/** Explicit additive upload; update only matching local URLs, never print credentials. */
class PublishCatalogImages {
 public static void main(String[] args)throws Exception {
  if(!List.of(args).contains("--apply"))throw new IllegalArgumentException("Use --apply to publish images and update database URLs");
  Properties app=new Properties(),db=new Properties();
  try(var r=Files.newBufferedReader(Path.of("src/main/resources/application.properties"))){app.load(r);}
  try(var r=Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))){db.load(r);}
  var cloud=new Cloudinary(Map.of("cloud_name",app.getProperty("cloudinary.cloud-name"),"api_key",app.getProperty("cloudinary.api-key"),"api_secret",app.getProperty("cloudinary.api-secret"),"secure",true));
  Path root=Path.of("src/main/resources/static").toAbsolutePath().normalize();
  var definitions=Collections.singletonList(new String[]{"products","image_url","image_public_id"});
  var uploaded=new LinkedHashMap<String,Map<String,String>>();int changed=0,missing=0;
  try(var connection=DriverManager.getConnection(db.getProperty("spring.datasource.url"),db.getProperty("spring.datasource.username"),db.getProperty("spring.datasource.password"))){
   if(!"utetra_db".equals(connection.getCatalog()))throw new IllegalStateException("Unexpected database");
   for(var def:definitions){
    List<String> urls=new ArrayList<>();try(var s=connection.prepareStatement("select distinct "+def[1]+" from "+def[0]+" where "+def[1]+" like '/assets/images/%'");var rows=s.executeQuery()){while(rows.next())urls.add(rows.getString(1));}
    for(String old:urls){
     if(!uploaded.containsKey(old)){
      Path file=root.resolve(old.substring(1)).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file)){missing++;System.out.println("MISSING "+old);continue;}
      String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
      var result=cloud.uploader().upload(file.toFile(),Map.of("public_id",app.getProperty("cloudinary.folder","utetra")+"/catalog/"+digest,"resource_type","image","overwrite",false));
      String url=Objects.toString(result.get("secure_url"),""),id=Objects.toString(result.get("public_id"),"");
      if(!url.startsWith("https://res.cloudinary.com/")||id.isBlank())throw new IllegalStateException("Invalid image upload response");
      uploaded.put(old,Map.of("url",url,"publicId",id));
      Files.writeString(Path.of(".local/cloudinary-image-manifest.json"),new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(uploaded));
     }
     var asset=uploaded.get(old);try(var update=connection.prepareStatement("update "+def[0]+" set "+def[1]+"=?,"+def[2]+"=? where "+def[1]+"=?")){update.setString(1,asset.get("url"));update.setString(2,asset.get("publicId"));update.setString(3,old);changed+=update.executeUpdate();}
    }
   }
   try(var optimize=connection.prepareStatement("update products set image_url=replace(image_url,'/image/upload/','/image/upload/f_auto,q_auto,w_1000/'),version=version+1,updated_at=CURRENT_TIMESTAMP(6) where image_public_id like ? and image_url like 'https://res.cloudinary.com/%/image/upload/v%'")){
    optimize.setString(1,app.getProperty("cloudinary.folder","utetra")+"/catalog/%");System.out.println("Optimized product image URLs="+optimize.executeUpdate());
   }
  }
  System.out.println("Published distinct images="+uploaded.size()+" updated database rows="+changed+" missing local images="+missing);
 }
}
