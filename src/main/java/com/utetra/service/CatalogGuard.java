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
    private final UserRepository users;
    private final StoreRepository stores;
    private final StoreMembershipRepository memberships;
    private final CatalogAuditRepository audit;
    public CatalogGuard(UserRepository users,StoreRepository stores,StoreMembershipRepository memberships,CatalogAuditRepository audit) {
        this.users=users; this.stores=stores; this.memberships=memberships; this.audit=audit;
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
    public void reviewer(UtetraPrincipal p,Long storeId) {
        if(!java.util.Set.of("ROLE_ADMIN","ROLE_MANAGER").contains(role(p))) forbidden();
        store(p,storeId);
    }
    /** Global definitions cannot be changed by a store-only manager assignment. */
    public void catalog(UtetraPrincipal p) {
        String role=role(p);
        if("ROLE_ADMIN".equals(role)) return;
        if(!"ROLE_MANAGER".equals(role) || !memberships.findAll().stream().anyMatch(m -> m.getUserId().equals(p.userId()) && m.getActive() && "CATALOG_ADMIN".equals(m.getMemberRole()))) forbidden();
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
