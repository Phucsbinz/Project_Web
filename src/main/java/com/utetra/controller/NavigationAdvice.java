package com.utetra.controller;
import com.utetra.security.UtetraPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@ControllerAdvice
public class NavigationAdvice {
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 public NavigationAdvice(org.springframework.jdbc.core.JdbcTemplate jdbc){this.jdbc=jdbc;}
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
}
