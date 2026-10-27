package com.utetra.support;
import com.utetra.identity.IdentityRepository;
import com.utetra.security.UtetraPrincipal;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class SupportRepository {
 private final IdentityRepository db; private final EntityManager em;
 public SupportRepository(IdentityRepository db,EntityManager em){this.db=db;this.em=em;}
 private static final String ELIGIBLE=" from support_conversations t join users u on u.id=t.customer_id join roles r on r.id=u.role_id left join stores s on s.id=t.store_id where u.is_active=true and u.email_verified_at is not null and r.name='ROLE_USER' and (t.channel='ADMIN' or s.status='ACTIVE')";
 private static final String ACCESS=" and ((?='ROLE_USER' and t.customer_id=?) or (?='ROLE_ADMIN' and t.channel='ADMIN') or (?='ROLE_MANAGER' and t.channel='BRANCH' and exists(select 1 from store_memberships sm where sm.store_id=t.store_id and sm.user_id=? and sm.is_active=true and sm.member_role in ('MANAGER','CATALOG_ADMIN'))))";
 private Object[] access(UtetraPrincipal p){return new Object[]{p.role(),p.userId(),p.role(),p.role(),p.userId()};}
 private Object[] args(UtetraPrincipal p,Object... extra){var values=new ArrayList<Object>(Arrays.asList(access(p)));values.addAll(Arrays.asList(extra));return values.toArray();}
 public List<Map<String,Object>> targets(){return db.list("select s.id,s.name from stores s where s.status='ACTIVE' and exists(select 1 from store_memberships sm join users u on u.id=sm.user_id join roles r on r.id=u.role_id where sm.store_id=s.id and sm.is_active=true and sm.member_role in ('MANAGER','CATALOG_ADMIN') and u.is_active=true and u.email_verified_at is not null and r.name='ROLE_MANAGER') order by s.name,s.id");}
 public boolean adminAvailable(){return ((Number)db.one("select count(*) n from users u join roles r on r.id=u.role_id where r.name='ROLE_ADMIN' and u.is_active=true and u.email_verified_at is not null").get("n")).longValue()>0;}
 public boolean targetAvailable(Long store){return store==null?adminAvailable():targets().stream().anyMatch(s->((Number)s.get("id")).longValue()==store);}
 public void lockCustomer(long id){db.one("select id from users where id=? for update",id);}
 public Map<String,Object> existing(long customer,String scope){var rows=db.list("select id from support_conversations where customer_id=? and scope_key=?",customer,scope);return rows.isEmpty()?null:rows.getFirst();}
 public List<Map<String,Object>> conversations(UtetraPrincipal p,int page){return db.list("select t.*,u.full_name customer_name,case when t.channel='ADMIN' then 'Admin UTETra' else s.name end title,(select body from support_messages m where m.id=t.last_message_id) last_body"+ELIGIBLE+ACCESS+" order by t.updated_at desc,t.id desc limit 31 offset ?",args(p,page*30));}
 public Map<String,Object> accessible(UtetraPrincipal p,String id,boolean lock){var rows=db.list("select t.*,u.full_name customer_name,case when t.channel='ADMIN' then 'Admin UTETra' else s.name end title"+ELIGIBLE+ACCESS+" and t.id=?"+(lock?" for update":""),args(p,id));return rows.isEmpty()?null:rows.getFirst();}
 public void insert(SupportConversation thread){em.persist(thread);em.flush();}
 public Map<String,Object> retry(String thread,long sender,String nonce){var rows=db.list("select id,body from support_messages where conversation_id=? and sender_id=? and client_message_id=?",thread,sender,nonce);return rows.isEmpty()?null:rows.getFirst();}
 public long recentCount(long sender,Instant after){return ((Number)db.one("select count(*) n from support_messages where sender_id=? and created_at>=?",sender,after).get("n")).longValue();}
 public void insert(SupportMessage message){em.persist(message);em.flush();db.update("update support_conversations set last_message_id=?,updated_at=? where id=?",message.id,message.createdAt,message.conversationId);}
 private static final String MESSAGE="select m.id,m.conversation_id,m.sender_id,m.sender_role,m.client_message_id,m.body,m.created_at,u.full_name sender_name from support_messages m join users u on u.id=m.sender_id";
 public Map<String,Object> message(String thread,long id){return db.one(MESSAGE+" where m.conversation_id=? and m.id=?",thread,id);}
 public List<Map<String,Object>> messages(String thread,Long before,Long after){return after!=null?db.list(MESSAGE+" where m.conversation_id=? and m.id>? order by m.id asc limit 51",thread,after):db.list(MESSAGE+" where m.conversation_id=? and m.id<? order by m.id desc limit 51",thread,before==null?Long.MAX_VALUE:before);}
 public List<Long> participants(String thread){var rows=db.list("select u.id from users u join roles r on r.id=u.role_id where u.is_active=true and u.email_verified_at is not null and exists(select 1 from support_conversations t join users c on c.id=t.customer_id join roles cr on cr.id=c.role_id where t.id=? and c.is_active=true and c.email_verified_at is not null and cr.name='ROLE_USER' and ((u.id=t.customer_id and r.name='ROLE_USER') or (t.channel='ADMIN' and r.name='ROLE_ADMIN') or (t.channel='BRANCH' and r.name='ROLE_MANAGER' and exists(select 1 from stores s join store_memberships sm on sm.store_id=s.id where s.id=t.store_id and s.status='ACTIVE' and sm.user_id=u.id and sm.is_active=true and sm.member_role in ('MANAGER','CATALOG_ADMIN')))))",thread);return rows.stream().map(row->((Number)row.get("id")).longValue()).toList();}
}
