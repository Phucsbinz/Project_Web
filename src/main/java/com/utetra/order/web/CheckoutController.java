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
  private final LocalQrPaymentService localQr;
  private final CheckoutAvailabilityService availability;

  public CheckoutController(CheckoutService checkout, OperationsService operations,
      CheckoutAvailabilityService availability,CartService carts,com.utetra.order.repository.CommerceReferenceRepository refs,LocalQrPaymentService localQr) {
    this.carts=carts;this.refs=refs;this.localQr=localQr;
    this.checkout = checkout;
    this.operations = operations;
    this.availability = availability;
  }

  @GetMapping
  public String page(@RequestParam(required=false) Long branch,Model model) {
    var cart=carts.view(OrderWebSupport.actor());
    if(((java.util.List<?>)cart.get("items")).isEmpty())return "redirect:/cart";
    var selected=((java.util.List<?>)cart.get("items")).stream().map(item->(Map<?,?>)item).filter(item->!Boolean.FALSE.equals(item.get("selected"))).toList();
    if(selected.isEmpty())return "redirect:/cart";
    model.addAttribute("groupPaymentMode",((Number)cart.getOrDefault("groupOrderId",0)).longValue()>0?refs.one("select payment_mode from group_orders where id=?",cart.get("groupOrderId")).get("payment_mode"):"HOST");
    if(((Number)cart.getOrDefault("groupOrderId",0)).longValue()>0)model.addAttribute("groupToken",refs.one("select invite_token from group_orders where id=?",cart.get("groupOrderId")).get("invite_token"));
    model.addAttribute("groupDiscountActive",((Number)cart.getOrDefault("groupOrderId",0)).longValue()>0 && ((Number)refs.one("select count(*) as participants from group_order_members where group_order_id=? and is_confirmed=true and items_json<>'[]'",cart.get("groupOrderId")).get("participants")).intValue()>=3);
    model.addAttribute("cartExpiresAt",cart.get("expiresAt"));
    model.addAttribute("cartVersion",cart.get("version"));
    model.addAttribute("branches",refs.list("select id,name,address from stores where status='ACTIVE' order by name,id"));
    model.addAttribute("branchReady",branch!=null && branch>0 && branch.equals(((Number)cart.get("storeId")).longValue()));
    model.addAttribute("choices", operations.checkoutChoices(OrderWebSupport.actor()));
    model.addAttribute("localQrEnabled", localQr.available());
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

  @PostMapping("/vouchers") @ResponseBody
  public Object vouchers(@RequestBody Checkout request){return checkout.vouchers(OrderWebSupport.actor(),request);}

  @GetMapping("/availability")
  @ResponseBody
  public Map<String,Object> availability() { return availability.snapshot(OrderWebSupport.actor()); }

  @PostMapping
  @ResponseBody
  public Map<String, Object> create(@Valid @RequestBody Checkout request) {
    return checkout.checkout(OrderWebSupport.actor(), request);
  }
}
