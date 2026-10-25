package com.utetra.order.web;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.service.OrderProblem;
import com.utetra.security.UtetraPrincipal;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class OrderWebSupport {
  private OrderWebSupport() {}

  public static Actor actor() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !(a.getPrincipal() instanceof UtetraPrincipal p))
      throw OrderProblem.forbidden();
    return new Actor(p.userId(), p.role());
  }

  public static Actor optional() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    return a != null && a.getPrincipal() instanceof UtetraPrincipal ? actor() : null;
  }

  public static Map<String, Object> carrier(com.utetra.order.entity.Carrier c) {
    var result=new LinkedHashMap<String,Object>(Map.of(
        "id", c.id, "name", c.name, "fee", c.baseFee, "active", c.isActive, "version", c.version));
    result.put("storeId",c.managedStoreId);return result;
  }
}
