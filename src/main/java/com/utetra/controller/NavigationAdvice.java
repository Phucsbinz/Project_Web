package com.utetra.controller;
import com.utetra.security.UtetraPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@ControllerAdvice
public class NavigationAdvice {
 @ModelAttribute("currentPath") public String path(jakarta.servlet.http.HttpServletRequest request) {return request.getServletPath();}
 @ModelAttribute("workspace") public boolean workspace(jakarta.servlet.http.HttpServletRequest request) {
  return request.getServletPath().matches("/(admin|manager|vendor|shipper)(/.*)?");
 }
 @ModelAttribute("currentRole") public String role(@AuthenticationPrincipal UtetraPrincipal principal) {
  return principal==null?"GUEST":principal.role();
 }
}
