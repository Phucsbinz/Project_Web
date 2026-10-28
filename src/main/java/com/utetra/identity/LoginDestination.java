package com.utetra.identity;

import java.net.URI;
import java.util.Set;

/** Local, role-appropriate entry points only; every destination retains backend scope checks. */
final class LoginDestination {
  private LoginDestination() {}
  static String resolve(String role,String next) {
    String home=switch(role) {
      case "ROLE_VENDOR" -> "/vendor";
      case "ROLE_MANAGER" -> "/manager";
      case "ROLE_ADMIN" -> "/admin";
      case "ROLE_SHIPPER" -> "/shipper/orders";
      default -> "/";
    };
    if(next==null || next.isBlank() || !next.startsWith("/") || next.startsWith("//")
        || next.contains("\\") || next.chars().anyMatch(c -> c<32 || c==127)) return home;
    try {
      URI uri=URI.create(next);
      String path=uri.getPath();
      if(uri.getRawPath().contains("%"))return home;
      if(uri.isAbsolute() || uri.getRawAuthority()!=null || uri.getRawFragment()!=null || !path.equals(uri.normalize().getPath())) return home;
      String root=path.split("/",3).length>1?path.split("/",3)[1]:"";
      boolean allowed=Set.of("account","notifications").contains(root)
          || "ROLE_ADMIN".equals(role) && "admin".equals(root)
          || "ROLE_MANAGER".equals(role) && "manager".equals(root)
          || "ROLE_VENDOR".equals(role) && "vendor".equals(root)
          || "ROLE_SHIPPER".equals(role) && "shipper".equals(root)
          || Set.of("ROLE_USER","ROLE_VENDOR").contains(role) && Set.of("","menu","stores","products","orders","cart","checkout","group-orders","favorites","viewed","reviews","comments").contains(root);
      return allowed?next:home;
    } catch(IllegalArgumentException ex) {return home;}
  }
}
