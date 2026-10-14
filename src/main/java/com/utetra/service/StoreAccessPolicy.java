package com.utetra.service;

import com.utetra.security.UtetraPrincipal;

/** Implement in the shop module using ownership/manager assignments. Fail closed until then. */
public interface StoreAccessPolicy {
    boolean canOperate(UtetraPrincipal principal,Long storeId);
}
