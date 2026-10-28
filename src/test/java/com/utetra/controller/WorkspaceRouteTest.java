package com.utetra.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.controller.publicweb.CatalogController;
import com.utetra.service.*;
import com.utetra.order.service.GroupOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
class WorkspaceRouteTest {
 @Test void roleRootsLeadToDedicatedWorkspaces() {
  var c=new CatalogController(mock(CatalogService.class),mock(StoreService.class),mock(PromotionService.class),new ObjectMapper(),mock(GroupOrderService.class));
  var r=new MockHttpServletRequest();r.setServletPath("/vendor");
  assertEquals("redirect:/vendor/overview",c.management(r,new ExtendedModelMap()));
  r.setServletPath("/manager");assertEquals("redirect:/manager/operations",c.management(r,new ExtendedModelMap()));
  r.setServletPath("/manager/products");assertEquals("redirect:/manager/operations",c.management(r,new ExtendedModelMap()));
 }
}
