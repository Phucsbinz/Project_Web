package com.utetra.order.web;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {
  private final CheckoutService checkout;
  private final OperationsService operations;

  public CheckoutController(CheckoutService checkout, OperationsService operations) {
    this.checkout = checkout;
    this.operations = operations;
  }

  @GetMapping
  public String page(Model model) {
    model.addAttribute("choices", operations.checkoutChoices(OrderWebSupport.actor()));
    return "order/checkout";
  }

  @PostMapping("/quote")
  @ResponseBody
  public Map<String, Object> quote(@RequestBody Checkout request) {
    return checkout.quote(OrderWebSupport.actor(), request);
  }

  @PostMapping
  @ResponseBody
  public Map<String, Object> create(@Valid @RequestBody Checkout request) {
    return checkout.checkout(OrderWebSupport.actor(), request);
  }
}
