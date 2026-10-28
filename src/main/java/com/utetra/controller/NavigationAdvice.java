package com.utetra.controller;
import com.utetra.security.UtetraPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@ControllerAdvice
public class NavigationAdvice {
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 private final org.springframework.context.ApplicationContext modules;
 public NavigationAdvice(org.springframework.jdbc.core.JdbcTemplate jdbc,org.springframework.context.ApplicationContext modules){this.jdbc=jdbc;this.modules=modules;}
 @ModelAttribute("commerceAvailable") public boolean commerceAvailable(){return modules.containsBean("cartService");}
 @ModelAttribute("currentDisplayName") public String displayName(@AuthenticationPrincipal UtetraPrincipal principal,jakarta.servlet.http.HttpServletRequest request) {
  if(principal==null||!"GET".equals(request.getMethod())||request.getHeader("Accept")!=null&&request.getHeader("Accept").contains("application/json"))return "";
  return jdbc.query("select full_name,username from users where id=?",(row,index)->{String name=row.getString("full_name");return name==null||name.isBlank()?row.getString("username"):name;},principal.userId()).stream().findFirst().orElse("Người dùng");
 }

 @ModelAttribute("currentPath") public String path(jakarta.servlet.http.HttpServletRequest request) {return request.getServletPath();}
 @ModelAttribute("workspace") public boolean workspace(jakarta.servlet.http.HttpServletRequest request) {
  return request.getServletPath().matches("/(admin|manager|vendor|shipper)(/.*)?");
 }
 @ModelAttribute("currentRole") public String role(@AuthenticationPrincipal UtetraPrincipal principal) {
  return principal==null?"GUEST":principal.role();
 }
 @ModelAttribute("currentUserId") public Long userId(@AuthenticationPrincipal UtetraPrincipal principal){return principal==null?null:principal.userId();}

 @ModelAttribute("catalogAdministration") public boolean catalogAdministration(@AuthenticationPrincipal UtetraPrincipal p,jakarta.servlet.http.HttpServletRequest req) {
  if(p==null)return false;
  if("ROLE_ADMIN".equals(p.role()))return true;
  if(!"ROLE_MANAGER".equals(p.role()) || !"GET".equals(req.getMethod()) || req.getServletPath().startsWith("/api/"))return false;
  return !jdbc.queryForList("select id from store_memberships where user_id=? and is_active=true and member_role='CATALOG_ADMIN'",p.userId()).isEmpty();
 }
 @ModelAttribute("workspaceTitle") public String workspaceTitle(@AuthenticationPrincipal UtetraPrincipal p,jakarta.servlet.http.HttpServletRequest req) {
  if(p==null)return "";
  return switch(p.role()) {
   case "ROLE_ADMIN" -> "Quản trị hệ thống";
   case "ROLE_VENDOR" -> "Chủ cửa hàng";
   case "ROLE_SHIPPER" -> "Giao hàng của bạn";
   case "ROLE_MANAGER" -> {
    if(!"GET".equals(req.getMethod())||req.getServletPath().startsWith("/api/"))yield "Quản lý chi nhánh";
    var stores=jdbc.queryForList("select s.id,s.name from stores s join store_memberships m on m.store_id=s.id where m.user_id=? and m.is_active=true and m.member_role in ('MANAGER','CATALOG_ADMIN') order by s.id",p.userId());
    String selected=req.getParameter("store");
    var branch=stores.stream().filter(s -> String.valueOf(s.get("id")).equals(selected)).findFirst();
    if(branch.isEmpty() && stores.size()==1)branch=java.util.Optional.of(stores.getFirst());
    yield branch.map(s -> "Quản lý "+s.get("name")).orElse(stores.isEmpty()?"Chưa được phân công":"Quản lý chi nhánh");
   }
   default -> "Mua hàng";
  };
 }
}
