package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import com.utetra.security.UtetraPrincipal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    private final IdentityRepository db; private final AccountService account; private final CommerceIdentityAdapter commerce; private final AuthService auth; private final Clock clock; private final AuditService audit;
    private final IdentityInputs inputs;
    public UserManagementService(IdentityRepository db,AccountService account,CommerceIdentityAdapter commerce,AuthService auth,Clock clock,AuditService audit,IdentityInputs inputs) { this.db=db; this.account=account; this.commerce=commerce; this.auth=auth; this.clock=clock; this.audit=audit; this.inputs=inputs; }
    private boolean admin(UtetraPrincipal p) {
        account.require(p,false);
        if(!Set.of("ROLE_MANAGER","ROLE_ADMIN").contains(p.role())) throw IdentityException.denied();
        return "ROLE_ADMIN".equals(p.role());
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> search(UtetraPrincipal p,String query,int page,int size) {
        if(query!=null && query.length()>100) throw IdentityException.invalid("Từ khóa tìm kiếm tối đa 100 ký tự.");
        boolean global=admin(p); int offset=IdentityValidation.offset(page,size);
        String q="%"+Objects.toString(query,"").replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String sql="select u.id,u.username,u.email,u.full_name,u.is_active,u.version,r.name as role_name from users u join roles r on r.id=u.role_id where (u.username like ? escape '!' or u.email like ? escape '!' or u.full_name like ? escape '!')";
        if(global) return db.list(sql+" order by u.id limit ? offset ?",q,q,q,size,offset);
        return db.list(sql+" and r.name in ('ROLE_USER','ROLE_VENDOR') and exists(select 1 from store_memberships m where m.user_id=? and m.is_active=true and (exists(select 1 from stores s where s.id=m.store_id and s.owner_user_id=u.id) or exists(select 1 from orders o where o.store_id=m.store_id and o.user_id=u.id))) order by u.id limit ? offset ?",q,q,q,p.userId(),size,offset);
    }
    @Transactional
    public void manage(UtetraPrincipal p,long id,IdentityForms.Manage form) {
        IdentityValidation.id(id); inputs.require(form);
        boolean global=admin(p);
        // Serialize all Admin removals/promotions using a stable row, not an unlocked count.
        db.one("select id from roles where name='ROLE_ADMIN' for update");
        account.require(p,false);
        var user=db.user(id,true); String current=string(user,"role_name");
        if(number(user,"version")!=form.version()) throw IdentityException.conflict();
        String requested=form.role()==null?current:form.role();
        if(!global && (!commerce.canManageUser(p.userId(),id) || !Set.of("ROLE_USER","ROLE_VENDOR").contains(current) || !requested.equals(current))) throw IdentityException.denied();
        if(current.equals("ROLE_ADMIN") && flag(user,"is_active") && user.get("email_verified_at")!=null && (!form.active() || !requested.equals("ROLE_ADMIN")) && db.count("select count(*) from users u join roles r on r.id=u.role_id where r.name='ROLE_ADMIN' and u.is_active=true and u.email_verified_at is not null")<=1) throw IdentityException.invalid("Phải giữ ít nhất một Admin hoạt động.");
        long roleId=number(db.one("select id from roles where name=?",requested),"id");
        audit.record(p,AuditService.Action.MANAGE_USER,AuditService.Target.USERS,id);
        db.update("update users set is_active=?,role_id=?,updated_at=?,version=version+1 where id=?",form.active(),roleId,time(clock.instant()),id);
        if(!requested.equals("ROLE_MANAGER")) db.update("update store_memberships set is_active=false,updated_at=? where user_id=?",time(clock.instant()),id);
        auth.revokeAll(id);
    }
    @Transactional
    public void assign(UtetraPrincipal p,long managerId,long storeId,boolean active) {
        IdentityValidation.id(managerId); IdentityValidation.id(storeId);
        if(!admin(p)) throw IdentityException.denied();
        var user=db.user(managerId,true);
        if(!"ROLE_MANAGER".equals(string(user,"role_name"))) throw IdentityException.invalid("Tài khoản chưa có vai trò Manager.");
        db.one("select id from stores where id=?",storeId);
        var now=time(clock.instant());
        if(db.update("update store_memberships set is_active=?,assigned_by=?,updated_at=? where user_id=? and store_id=?",active,p.userId(),now,managerId,storeId)==0)
            db.update("insert into store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at) values(?,?,'MANAGER',?,?,?,?)",managerId,storeId,active,p.userId(),now,now);
        audit.record(p,AuditService.Action.ASSIGN_MANAGER,AuditService.Target.STORES,storeId); auth.revokeAll(managerId);
    }
    @Transactional
    public void hide(UtetraPrincipal p,boolean review,long id) {
        IdentityValidation.id(id);
        if(!admin(p)) throw IdentityException.denied();
        String table=review?"reviews":"product_comments";
        if(db.update("update "+table+" set status='HIDDEN',updated_at=?,version=version+1 where id=?",time(clock.instant()),id)!=1) throw IdentityException.missing();
        audit.record(p,AuditService.Action.HIDE_COMMUNITY,review?AuditService.Target.REVIEWS:AuditService.Target.PRODUCT_COMMENTS,id);
    }
}
