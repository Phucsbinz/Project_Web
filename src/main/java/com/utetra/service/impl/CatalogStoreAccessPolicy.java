package com.utetra.service.impl;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class CatalogStoreAccessPolicy implements StoreAccessPolicy {
    private final CatalogGuard guard;
    public CatalogStoreAccessPolicy(CatalogGuard guard) { this.guard=guard; }
    public boolean canOperate(UtetraPrincipal principal,Long storeId) {
        try { return principal!=null && storeId!=null && guard.operates(principal,storeId); }
        catch(org.springframework.web.server.ResponseStatusException ex) { return false; }
    }
}
