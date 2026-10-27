package com.utetra.order.web;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
public class CartController {
  private final CartService carts;
  private final GuestCartService guests;
  private final GroupOrderService groups;

  public CartController(CartService carts, GuestCartService guests,GroupOrderService groups) {
    this.carts = carts;
    this.guests = guests;this.groups=groups;
  }

  private GuestCartService.Snapshot guest(HttpSession session) {
    synchronized (session) {
      var g = (GuestCartService.Snapshot) session.getAttribute("utetra.cart");
      if (g == null) {
        g = new GuestCartService.Snapshot();
        session.setAttribute("utetra.cart", g);
      }
      guests.expire(g);return g;
    }
  }

  @GetMapping
  public String page(Model model, HttpServletRequest request) {
    model.addAttribute("cart", view(request));
    model.addAttribute("signedIn", OrderWebSupport.optional() != null);
    model.addAttribute("groups",OrderWebSupport.optional()==null?List.of():groups.mine(OrderWebSupport.actor()));
    var items=(List<?>)((Map<?,?>)model.getAttribute("cart")).get("items");
    java.math.BigDecimal subtotal=java.math.BigDecimal.ZERO;
    for(var raw:items){var item=(Map<?,?>)raw;if(!Boolean.FALSE.equals(item.get("selected"))&&item.get("subtotal")!=null)subtotal=subtotal.add(new java.math.BigDecimal(item.get("subtotal").toString()));}
    model.addAttribute("subtotal",subtotal);
    return "order/cart";
  }

  @GetMapping("/data")
  @ResponseBody
  public Map<String, Object> view(HttpServletRequest request) {
    Actor actor = OrderWebSupport.optional();
    return actor != null ? carts.view(actor) : guests.view(guest(request.getSession()));
  }

  @GetMapping("/count") @ResponseBody
  public Map<String,Object> count(HttpServletRequest request) {
    Actor actor=OrderWebSupport.optional();
    if(actor!=null)return carts.count(actor);
    var snapshot=guest(request.getSession());synchronized(snapshot){return Map.of("count",snapshot.items.size(),"cups",snapshot.items.stream().mapToInt(Item::quantity).sum());}
  }

  public record Selection(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max=50) List<@jakarta.validation.constraints.NotBlank String> ids,@jakarta.validation.constraints.PositiveOrZero long version){}
  @PostMapping("/selection") @ResponseBody
  public Map<String,Object> selection(@Valid @RequestBody Selection input,HttpServletRequest request){
    var actor=OrderWebSupport.optional();if(actor==null)return guests.select(guest(request.getSession()),Set.copyOf(input.ids()),input.version());
    try{return carts.select(actor,input.ids().stream().map(Long::parseLong).collect(java.util.stream.Collectors.toSet()),input.version());}catch(NumberFormatException error){throw OrderProblem.invalid("Mã món không hợp lệ.");}
  }

  @PostMapping("/draft/items") @ResponseBody
  public Map<String,Object> addDraft(@Valid @RequestBody Item item,HttpServletRequest request) {
    Actor actor=OrderWebSupport.optional();return actor!=null?carts.addDraft(actor,item):guests.addDraft(guest(request.getSession()),item);
  }

  @PostMapping("/items")
  @ResponseBody
  public Map<String, Object> add(@Valid @RequestBody Item item, HttpServletRequest request) {
    Actor actor = OrderWebSupport.optional();
    return actor != null ? carts.add(actor, item) : guests.add(guest(request.getSession()), item);
  }

  @PostMapping("/items/{id}")
  @ResponseBody
  public Map<String, Object> quantity(
      @PathVariable String id, @Valid @RequestBody Quantity value, HttpServletRequest request) {
    Actor actor = OrderWebSupport.optional();
    return actor != null
        ? carts.change(actor, Long.parseLong(id), value.quantity(), value.version(), false)
        : guests.change(guest(request.getSession()), id, value.quantity(), value.version(), false);
  }

  @PostMapping("/items/{id}/delete")
  @ResponseBody
  public Map<String, Object> delete(
      @PathVariable String id, @Valid @RequestBody VersionInput value, HttpServletRequest request) {
    Actor actor = OrderWebSupport.optional();
    return actor != null
        ? carts.change(actor, Long.parseLong(id), 0, value.version(), true)
        : guests.change(guest(request.getSession()), id, 0, value.version(), true);
  }

  @PostMapping("/clear")
  @ResponseBody
  public Map<String, Object> clear(HttpServletRequest request) {
    Actor actor = OrderWebSupport.optional();
    if (actor != null) carts.clear(actor);
    request.getSession().removeAttribute("utetra.cart");
    return view(request);
  }

  @PostMapping("/merge")
  @ResponseBody
  public Map<String, Object> merge(HttpServletRequest request) {
    Actor actor = OrderWebSupport.actor();
    var g = guest(request.getSession());
    Map<String, Object> result;
    synchronized (g) {
      guests.expire(g);
      result = g.draft?carts.mergeDraft(actor,List.copyOf(g.items),g.mergeKey):carts.merge(actor, List.copyOf(g.items), g.mergeKey);
    }
    if(!g.unselected.isEmpty())result=carts.applyGuestExclusions(actor,Set.copyOf(g.unselected));
    request.getSession().removeAttribute("utetra.cart");
    return result;
  }
}
