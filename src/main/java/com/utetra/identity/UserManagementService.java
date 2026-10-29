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
    @Transactional
    public long create(UtetraPrincipal p,IdentityForms.CreateAccount form) {
        if(!admin(p)) throw IdentityException.denied();
        inputs.require(form);
        // Reuse BCrypt, uniqueness validation and email OTP; never bypass email ownership verification.
        auth.register(new IdentityForms.Register(form.username(),form.email(),form.password(),form.fullName()));
        long id=number(db.one("select id from users where email=?",IdentityValidation.email(form.email())),"id");
        db.update("update users set role_id=(select id from roles where name=?) where id=?",form.role(),id);
        audit.record(p,AuditService.Action.MANAGE_USER,AuditService.Target.USERS,id);
        return id;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> search(UtetraPrincipal p,String query,int page,int size) {
        return search(p,query,"",page,size);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> search(UtetraPrincipal p,String query,String role,int page,int size) {
        if(query!=null && query.length()>100) throw IdentityException.invalid("Từ khóa tìm kiếm tối đa 100 ký tự.");
        if(role!=null && !role.isBlank() && !Set.of("ROLE_USER","ROLE_VENDOR","ROLE_MANAGER","ROLE_ADMIN","ROLE_SHIPPER").contains(role)) throw IdentityException.invalid("Vai trò không hợp lệ.");
        boolean global=admin(p); int offset=IdentityValidation.offset(page,size);
        String q="%"+Objects.toString(query,"").replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String sql="select u.id,u.username,u.email,u.full_name,u.is_active,u.email_verified_at,u.version,r.name as role_name from users u join roles r on r.id=u.role_id where (u.username like ? escape '!' or u.email like ? escape '!' or u.full_name like ? escape '!')";
        List<Object> args=new ArrayList<>(List.of(q,q,q));
        if(role!=null && !role.isBlank()) { sql+=" and r.name=?"; args.add(role); }
        if(!global) {
            sql+=" and r.name in ('ROLE_USER','ROLE_VENDOR') and exists(select 1 from store_memberships m where m.user_id=? and m.is_active=true and (exists(select 1 from stores s where s.id=m.store_id and s.owner_user_id=u.id) or exists(select 1 from orders o where o.store_id=m.store_id and o.user_id=u.id)))";
            args.add(p.userId());
        }
        args.add(size); args.add(offset);
        return db.list(sql+" order by u.id limit ? offset ?",args.toArray());
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
        // Account lock above serializes this check with Shipper mutations/assignment.
        // A picked-up parcel cannot be handed over by the current delivery workflow.
        if(current.equals("ROLE_SHIPPER") && (!form.active() || !requested.equals("ROLE_SHIPPER"))
            && db.count("select count(*) from delivery_assignments a join orders o on o.id=a.order_id where a.shipper_id=? and a.status='ACTIVE' and o.status in ('PICKED_UP','DELIVERING')",id)>0)
            throw IdentityException.invalid("Shipper có đơn đang giao. Hãy hoàn tất giao hàng trước khi vô hiệu hóa hoặc đổi vai trò; các đơn chưa lấy hàng cần được phân công lại.");
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
        if(!"ROLE_MANAGER".equals(string(user,"role_name")) || active && (!flag(user,"is_active") || user.get("email_verified_at")==null)) throw IdentityException.invalid("Manager phải đang hoạt động và đã xác minh email.");
        var prior=db.optional("select member_role,is_active from store_memberships where user_id=? and store_id=? for update",managerId,storeId);
        if(active && prior.isPresent() && "CATALOG_ADMIN".equals(string(prior.get(),"member_role")) && !flag(prior.get(),"is_active"))
            throw IdentityException.invalid("Hãy dùng form phân công của Admin để xác nhận rõ quyền quản trị danh mục khi cấp lại.");
        db.one("select id from stores where id=?",storeId);
        var now=time(clock.instant());
        if(db.update("update store_memberships set is_active=?,assigned_by=?,updated_at=? where user_id=? and store_id=?",active,p.userId(),now,managerId,storeId)==0)
            db.update("insert into store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at) values(?,?,'MANAGER',?,?,?,?)",managerId,storeId,active,p.userId(),now,now);
        audit.record(p,AuditService.Action.ASSIGN_MANAGER,AuditService.Target.STORES,storeId); auth.revokeAll(managerId);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> moderation(UtetraPrincipal p,boolean review,String query,String status,int page,int size) {
        if(!admin(p)) throw IdentityException.denied();
        if(query!=null && query.length()>100) throw IdentityException.invalid("Từ khóa tối đa 100 ký tự.");
        if(status!=null && !status.isBlank() && !Set.of("VISIBLE","HIDDEN").contains(status)) throw IdentityException.invalid("Trạng thái không hợp lệ.");
        int offset=IdentityValidation.offset(page,size);
        String table=review?"reviews":"product_comments";
        String q="%"+Objects.toString(query,"").replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String sql="select x.id,x.author_id,x.order_item_id,x.product_id,x.content,x.status,x.version,x.created_at,u.full_name as author_name,p.name as product_name,oi.order_id"+(review?",x.stars":"")+" from "+table+" x join users u on u.id=x.author_id join products p on p.id=x.product_id join order_items oi on oi.id=x.order_item_id where (x.content like ? escape '!' or u.full_name like ? escape '!' or p.name like ? escape '!')";
        List<Object> args=new ArrayList<>(List.of(q,q,q));
        if(status!=null && !status.isBlank()) {sql+=" and x.status=?";args.add(status);}
        args.add(size);args.add(offset);
        return db.list(sql+" order by x.id desc limit ? offset ?",args.toArray());
    }
    @Transactional
    public void visibility(UtetraPrincipal p,boolean review,long id,boolean visible,long version) {
        IdentityValidation.id(id);IdentityValidation.version(version);
        if(!admin(p)) throw IdentityException.denied();
        String table=review?"reviews":"product_comments";
        var row=db.one("select * from "+table+" where id=? for update",id);
        if(number(row,"version")!=version) throw IdentityException.conflict();
        if(visible) {
            // Author removal blanks text and removes media: never resurrect that content.
            IdentityValidation.text(string(row,"content"),review?1:50,2000);
            if(review && (number(row,"stars")<1 || number(row,"stars")>5)) throw IdentityException.invalid("Đánh giá không hợp lệ để khôi phục.");
            if(db.count("select count(*) from order_items i join orders o on o.id=i.order_id join product_variants v on v.id=i.variant_id where i.id=? and o.user_id=? and v.product_id=?",number(row,"order_item_id"),number(row,"author_id"),number(row,"product_id"))!=1)
                throw IdentityException.invalid("Không thể xác minh bằng chứng mua hàng của nội dung.");
        }
        db.update("update "+table+" set status=?,updated_at=?,version=version+1 where id=?",visible?"VISIBLE":"HIDDEN",time(clock.instant()),id);
        audit.record(p,visible?AuditService.Action.RESTORE_COMMUNITY:AuditService.Action.HIDE_COMMUNITY,review?AuditService.Target.REVIEWS:AuditService.Target.PRODUCT_COMMENTS,id);
    }
    @Transactional
    public void hide(UtetraPrincipal p,boolean review,long id) {
        IdentityValidation.id(id);
        if(!admin(p)) throw IdentityException.denied();
        String table=review?"reviews":"product_comments";
        var row=db.one("select version from "+table+" where id=? for update",id);
        visibility(p,review,id,false,number(row,"version"));
    }
}
