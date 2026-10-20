package com.utetra.order.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(basePackages = "com.utetra.order.web")
public class OrderNavigation {
  @org.springframework.beans.factory.annotation.Value("${features.shipper.enabled:false}")
  private boolean shipperEnabled;

  @ModelAttribute
  public void navigation(Model model) {
    var actor = OrderWebSupport.optional();
    model.addAttribute("orderRole", actor == null ? "GUEST" : actor.role());
    model.addAttribute("shipperEnabled", shipperEnabled);
  }
}
