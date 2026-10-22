package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.StoreAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@org.springframework.context.annotation.Primary
public class IdentityStoreAccessPolicy implements StoreAccessPolicy {
    private final IdentityRepository db; private final AccountService accounts;
    public IdentityStoreAccessPolicy(IdentityRepository db,AccountService accounts) { this.db=db; this.accounts=accounts; }
    @Override @Transactional(readOnly=true)
    public boolean canOperate(UtetraPrincipal principal,Long storeId) {
        if(storeId==null || storeId<=0) return false;
        try {
            accounts.require(principal,false);
            return switch(principal.role()) {
                case "ROLE_ADMIN" -> true;
                case "ROLE_VENDOR" -> db.count("select count(*) from stores where id=? and owner_user_id=?",storeId,principal.userId())>0;
                case "ROLE_MANAGER" -> db.count("select count(*) from store_memberships where store_id=? and user_id=? and member_role IN ('MANAGER','CATALOG_ADMIN') and is_active=true",storeId,principal.userId())>0;
                default -> false;
            };
        } catch(IdentityException | org.springframework.dao.DataAccessException ex) { return false; }
    }
}
