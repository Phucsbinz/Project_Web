package com.utetra.service;

import com.utetra.security.UtetraPrincipal;
import com.utetra.repository.UserRepository;
import com.utetra.repository.catalog.*;
import com.utetra.entity.catalog.CatalogAudit;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogGuard {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final UserRepository users;
    private final StoreRepository stores;
    private final StoreMembershipRepository memberships;
    private final CatalogAuditRepository audit;
    public CatalogGuard(UserRepository users,StoreRepository stores,StoreMembershipRepository memberships,CatalogAuditRepository audit,org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.users=users; this.stores=stores; this.memberships=memberships; this.audit=audit; this.jdbc=jdbc;
    }
    public String role(UtetraPrincipal p) {
        if(p==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập");
        var user=users.findById(p.userId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!user.isActive() || user.getEmailVerifiedAt()==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user.getRole().getName();
    }
    public void buyer(UtetraPrincipal p) {
        if(!java.util.Set.of("ROLE_USER","ROLE_VENDOR").contains(role(p))) forbidden();
    }
    public boolean operates(UtetraPrincipal p,Long storeId) {
        String role=role(p);
        if("ROLE_ADMIN".equals(role)) return true;
        if("ROLE_MANAGER".equals(role)) return memberships.existsByUserIdAndStoreIdAndMemberRoleAndActiveTrue(p.userId(),storeId,"MANAGER") || memberships.existsByUserIdAndStoreIdAndMemberRoleAndActiveTrue(p.userId(),storeId,"CATALOG_ADMIN");
        return "ROLE_VENDOR".equals(role) && stores.findById(storeId).map(s -> s.getOwnerUserId().equals(p.userId())).orElse(false);
    }
    public void store(UtetraPrincipal p,Long storeId) {
        if(!operates(p,storeId)) forbidden();
        found(stores.findById(storeId));
    }
    public void admin(UtetraPrincipal p) { if(!"ROLE_ADMIN".equals(role(p))) forbidden(); }
    /** Authorization decision for a mutation. Revoke and mutate share the target user mutex. */
    private String lockedRole(UtetraPrincipal p) {
        if(p==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var rows=jdbc.queryForList("select role_id,is_active,email_verified_at from users where id=? for update",p.userId());
        if(rows.isEmpty()) forbidden();
        var row=rows.getFirst();
        if(!Boolean.TRUE.equals(row.get("is_active")) && !(row.get("is_active") instanceof Number n && n.intValue()!=0)) forbidden();
        if(row.get("email_verified_at")==null) forbidden();
        return jdbc.queryForObject("select name from roles where id=?",String.class,row.get("role_id"));
    }
    public void assignmentTarget(Long userId,boolean granting) {
        var rows=jdbc.queryForList("select role_id,is_active,email_verified_at from users where id=? for update",userId);
        require(!rows.isEmpty(),"Không tìm thấy Manager");var row=rows.getFirst();
        boolean active=Boolean.TRUE.equals(row.get("is_active")) || row.get("is_active") instanceof Number n && n.intValue()!=0;
        require("ROLE_MANAGER".equals(jdbc.queryForObject("select name from roles where id=?",String.class,row.get("role_id"))) && (!granting || active && row.get("email_verified_at")!=null),"Manager được cấp quyền phải đang hoạt động và đã xác minh email");
    }
    public void mutation(UtetraPrincipal p,Long storeId) {
        String current=lockedRole(p);
        if("ROLE_ADMIN".equals(current)) { found(stores.findById(storeId)); return; }
        if("ROLE_VENDOR".equals(current) && stores.findById(storeId).map(s->s.getOwnerUserId().equals(p.userId())).orElse(false)) return;
        if("ROLE_MANAGER".equals(current) && !jdbc.queryForList("select id from store_memberships where user_id=? and store_id=? and is_active=true and member_role in ('MANAGER','CATALOG_ADMIN') for update",p.userId(),storeId).isEmpty()) return;
        forbidden();
    }
    /** Ownership, not membership, grants business/price/promotion authority. */
    public void business(UtetraPrincipal p,Long storeId) {
        String current=lockedRole(p);
        if(!java.util.Set.of("ROLE_ADMIN","ROLE_VENDOR").contains(current)) forbidden();
        store(p,storeId);
    }
    public void reviewer(UtetraPrincipal p,Long storeId) {
        if(!java.util.Set.of("ROLE_ADMIN","ROLE_MANAGER").contains(role(p))) forbidden();
        store(p,storeId);
    }
    /** Global definitions cannot be changed by a store-only manager assignment. */
    public void catalog(UtetraPrincipal p) {
        boolean write=org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() && !org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        String role=write?lockedRole(p):role(p);
        if("ROLE_ADMIN".equals(role)) return;
        if(!"ROLE_MANAGER".equals(role) || jdbc.queryForList("select id from store_memberships where user_id=? and is_active=true and member_role='CATALOG_ADMIN'"+(write?" for update":""),p.userId()).isEmpty()) forbidden();
    }
    public void audit(UtetraPrincipal p,String action,String type,Long id) {
        var entry=new CatalogAudit(); entry.setActorId(p.userId()); entry.setAction(action); entry.setEntityType(type);
        entry.setEntityId(id); entry.setSummary(action+" "+type+" #"+id); audit.save(entry);
    }
    public static void forbidden() { throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền trong phạm vi này"); }
    public static void require(boolean condition,String message) { if(!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    public static void expectedVersion(long current,Long supplied) {
        if(supplied==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Thiếu phiên bản dữ liệu. Hãy tải lại trang trước khi lưu.");
        version(current,supplied.longValue());
    }
    public static void version(long current,long supplied) { if(current!=supplied) throw new ResponseStatusException(HttpStatus.CONFLICT,"Dữ liệu đã thay đổi. Hãy tải lại trang."); }
    public static <T> T found(java.util.Optional<T> value) { return value.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu")); }
}
