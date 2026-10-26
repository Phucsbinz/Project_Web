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
  private final CartService carts;
  private final com.utetra.order.repository.CommerceReferenceRepository refs;
  private final OperationsService operations;
  private final BankTransferSettings bank;
  private final VnpaySettings vnpay;
  private final CheckoutAvailabilityService availability;

  public CheckoutController(CheckoutService checkout, OperationsService operations, BankTransferSettings bank,
      CheckoutAvailabilityService availability,VnpaySettings vnpay,CartService carts,com.utetra.order.repository.CommerceReferenceRepository refs) {
    this.carts=carts;this.refs=refs;
    this.checkout = checkout;
    this.operations = operations;
    this.bank = bank;
    this.availability = availability;this.vnpay=vnpay;
  }

  @GetMapping
  public String page(@RequestParam(required=false) Long branch,Model model) {
    var cart=carts.view(OrderWebSupport.actor());
    if(((java.util.List<?>)cart.get("items")).isEmpty())return "redirect:/cart";
    model.addAttribute("cartVersion",cart.get("version"));
    model.addAttribute("branches",refs.list("select id,name,address from stores where status='ACTIVE' order by name,id"));
    model.addAttribute("branchReady",branch!=null && branch>0 && branch.equals(((Number)cart.get("storeId")).longValue()));
    model.addAttribute("choices", operations.checkoutChoices(OrderWebSupport.actor()));
    model.addAttribute("bankEnabled", bank.available());
    model.addAttribute("vnpayEnabled",vnpay.available());
    model.addAttribute("availability", availability.snapshot(OrderWebSupport.actor()));
    return "order/checkout";
  }

  @PostMapping("/branch") @ResponseBody
  public Map<String,Object> branch(@Valid @RequestBody BranchSelection request) {
    return carts.selectBranch(OrderWebSupport.actor(),request.storeId(),request.version());
  }

  @PostMapping("/quote")
  @ResponseBody
  public Map<String, Object> quote(@RequestBody Checkout request) {
    return checkout.quote(OrderWebSupport.actor(), request);
  }

  @GetMapping("/availability")
  @ResponseBody
  public Map<String,Object> availability() { return availability.snapshot(OrderWebSupport.actor()); }

  @PostMapping
  @ResponseBody
  public Map<String, Object> create(@Valid @RequestBody Checkout request) {
    return checkout.checkout(OrderWebSupport.actor(), request);
  }
}
