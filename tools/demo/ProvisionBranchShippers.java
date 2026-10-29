import com.utetra.UtetraApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Explicit additive test-account provisioning. Never resets or reactivates an existing account. */
public class ProvisionBranchShippers {
 public static void main(String[] args)throws Exception{
  boolean apply=List.of(args).contains("--apply");String password=System.getenv("UTETRA_SHIPPER_PASSWORD");
  if(apply&&(password==null||password.isBlank()))throw new IllegalArgumentException("Set UTETRA_SHIPPER_PASSWORD for new accounts only.");
  try(var ctx=new SpringApplicationBuilder(UtetraApplication.class).run("--spring.profiles.active=mysql","--server.port=0","--server.address=127.0.0.1","--app.mail.enabled=false","--spring.main.banner-mode=off")){
   var db=ctx.getBean(JdbcTemplate.class);if(!"utetra_db".equals(db.queryForObject("select database()",String.class)))throw new IllegalStateException("Unexpected database");
   var stores=db.queryForList("select id,name from stores where status='ACTIVE' order by id");
   if(!apply){System.out.println("SHIPPER_PLAN activeBranches="+stores.size()+" accountsPerBranch=3");return;}
   var tx=new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));String hash=ctx.getBean(PasswordEncoder.class).encode(password);
   long role=db.queryForObject("select id from roles where name='ROLE_SHIPPER'",Long.class);
   long admin=db.queryForObject("select min(u.id) from users u join roles r on r.id=u.role_id where r.name='ROLE_ADMIN' and u.is_active=true and u.email_verified_at is not null",Long.class);
   var records=new ArrayList<Map<String,Object>>();String[] names={"Nguyễn Minh Khang","Trần Quốc Huy","Lê Hoàng Phúc","Phạm Tuấn Kiệt","Võ Thành Đạt","Đặng Gia Bảo","Bùi Đức Anh","Đỗ Quang Vinh","Huỳnh Nhật Nam"};
   tx.executeWithoutResult(status->{int index=0;for(var store:stores){long storeId=((Number)store.get("id")).longValue();for(int member=1;member<=3;member++){
    String username="shipper.branch."+storeId+"."+member,name=names[index++%names.length],email=com.utetra.identity.TestAccountEmails.email(name,"ROLE_SHIPPER");Instant now=Instant.now();
    var found=db.queryForList("select u.id,u.email,u.full_name,u.is_active,r.name role_name from users u join roles r on r.id=u.role_id where u.username=?",username);long user;
    if(found.isEmpty()){final String createdName=name,createdEmail=email;var key=new GeneratedKeyHolder();db.update(c->{var s=c.prepareStatement("insert into users(role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version) values(?,?,?,?,?,true,?,?,?,0)",Statement.RETURN_GENERATED_KEYS);s.setLong(1,role);s.setString(2,username);s.setString(3,createdEmail);s.setString(4,hash);s.setString(5,createdName);for(int i=6;i<=8;i++)s.setTimestamp(i,Timestamp.from(now));return s;},key);user=Objects.requireNonNull(key.getKey()).longValue();}
    else{var prior=found.getFirst();if(!"ROLE_SHIPPER".equals(prior.get("role_name")))throw new IllegalStateException("Reserved provision identity has different role");user=((Number)prior.get("id")).longValue();name=Objects.toString(prior.get("full_name"));email=Objects.toString(prior.get("email"));}
    if(db.queryForObject("select count(*) from shipper_store_scopes where shipper_id=? and store_id=?",Long.class,user,storeId)==0){db.update("insert into shipper_store_scopes(shipper_id,store_id,is_active,updated_at,version) values(?,?,true,?,0)",user,storeId,Timestamp.from(now));db.update("insert into shipper_scope_events(shipper_id,store_id,actor_id,is_active,reason,created_at) values(?,?,?,true,?,?)",user,storeId,admin,"Bổ sung ba tài khoản Shipper cho chi nhánh theo yêu cầu chủ dự án.",Timestamp.from(now));}
    records.add(Map.of("id",user,"name",name,"email",email,"storeId",storeId,"storeName",store.get("name")));
   }}});
   Files.createDirectories(Path.of(".local"));Files.writeString(Path.of(".local/branch-shippers.json"),new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(records));System.out.println("SHIPPER_PROVISION entries="+records.size()+"; report=.local/branch-shippers.json; credentials never printed");
  }
 }
}
