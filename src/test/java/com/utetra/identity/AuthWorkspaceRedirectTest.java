package com.utetra.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class AuthWorkspaceRedirectTest {
  private String redirect(String role, String next) {
    AuthService auth=mock(AuthService.class);
    when(auth.login("owner@example.test","password")).thenReturn(new AuthService.Tokens("access","refresh",role));
    var controller=new AuthController(auth,mock(OtpService.class),mock(AuthCookies.class),mock(IdentityRateLimiter.class));
    var req=new MockHttpServletRequest(); req.setContextPath("/tea");
    if(next!=null) req.setParameter("next",next);
    return controller.login(new IdentityForms.Login("owner@example.test","password"),req,new MockHttpServletResponse()).get("redirect").toString();
  }
  @Test void staffLandInTheirOwnWorkspace() {
    assertEquals("/tea/vendor",redirect("ROLE_VENDOR",null));
    assertEquals("/tea/manager",redirect("ROLE_MANAGER",null));
    assertEquals("/tea/shipper/orders",redirect("ROLE_SHIPPER",null));
    assertEquals("/tea/admin",redirect("ROLE_ADMIN",null));
    assertEquals("/tea/",redirect("ROLE_USER",null));
  }
  @Test void preserveAuthorizedLocalDeepLink() {
    assertEquals("/tea/vendor/revenue?from=2026-10-01",redirect("ROLE_VENDOR","/vendor/revenue?from=2026-10-01"));
    assertEquals("/tea/group-orders/new",redirect("ROLE_USER","/group-orders/new"));
    assertEquals("/tea/menu?keyword=tr%C3%A0",redirect("ROLE_USER","/menu?keyword=tr%C3%A0"));
    assertEquals("/tea/manager/orders?store=3",redirect("ROLE_MANAGER","/manager/orders?store=3"));
  }
  @Test void rejectForeignAndCrossRoleDestinations() {
    for(String next:new String[]{"//evil.test","https://evil.test","/admin/orders","/vendor/../admin","/vendor/%2e%2e/admin","/vendor\\evil","/auth/logout"})
      assertEquals("/tea/vendor",redirect("ROLE_VENDOR",next),next);
  }
}
