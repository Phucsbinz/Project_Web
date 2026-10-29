import com.utetra.UtetraApplication;
import com.utetra.identity.TestAccountEmails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;

/** Explicit additive identity rename. Preserves users, roles, hashes, verified state and all foreign keys. */
public class RenameTestEmails {
 public static void main(String[] args)throws Exception{
  boolean apply=List.of(args).contains("--apply");
  try(var ctx=new SpringApplicationBuilder(UtetraApplication.class).run("--spring.profiles.active=mysql","--server.port=0","--server.address=127.0.0.1","--app.mail.enabled=false","--spring.main.banner-mode=off")){
   var db=ctx.getBean(JdbcTemplate.class);if(!"utetra_db".equals(db.queryForObject("select database()",String.class)))throw new IllegalStateException("Unexpected DB");
   var tx=new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));var result=new ArrayList<Map<String,Object>>();
   tx.executeWithoutResult(status->{
    var users=db.queryForList("select u.id,u.username,u.email,u.full_name,r.name role_name from users u join roles r on r.id=u.role_id order by u.id for update");Set<String> used=new HashSet<>();users.stream().filter(u->!TestAccountEmails.isTest(u.get("email").toString())).forEach(u->used.add(u.get("email").toString().toLowerCase(Locale.ROOT)));
    for(var u:users){String old=u.get("email").toString();if(!TestAccountEmails.isTest(old))continue;String target=TestAccountEmails.email(Objects.toString(u.get("full_name")),u.get("role_name").toString()),base=target;int duplicate=1;while(!used.add(target.toLowerCase(Locale.ROOT))){int split=base.indexOf('.');target=base.substring(0,split)+(++duplicate)+base.substring(split);}result.add(Map.of("id",u.get("id"),"name",u.get("full_name"),"username",u.get("username"),"role",u.get("role_name"),"oldEmail",old,"email",target));}
    if(!apply)return;
    // Two passes avoid unique-email swaps without changing any identity foreign keys.
    String run=UUID.randomUUID().toString().replace("-","");for(var row:result)if(!row.get("oldEmail").equals(row.get("email")))db.update("update users set email=? where id=?","rename."+row.get("id")+"."+run+"@example.test",row.get("id"));
    for(var row:result)if(!row.get("oldEmail").equals(row.get("email")))db.update("update users set email=?,updated_at=?,version=version+1 where id=?",row.get("email"),Timestamp.from(Instant.now()),row.get("id"));
   });
   Files.createDirectories(Path.of(".local"));Files.writeString(Path.of(".local/test-account-emails.json"),new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
   System.out.println("TEST_EMAILS "+(apply?"APPLIED":"PLAN")+" accounts="+result.size()+" report=.local/test-account-emails.json; real/SMTP emails untouched");
  }
 }
}
