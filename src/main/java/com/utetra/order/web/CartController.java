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

  public CartController(CartService carts, GuestCartService guests) {
    this.carts = carts;
    this.guests = guests;
  }

  private GuestCartService.Snapshot guest(HttpSession session) {
    synchronized (session) {
      var g = (GuestCartService.Snapshot) session.getAttribute("utetra.cart");
      if (g == null) {
        g = new GuestCartService.Snapshot();
        session.setAttribute("utetra.cart", g);
      }
      return g;
    }
  }

  @GetMapping
  public String page(Model model, HttpServletRequest request) {
    model.addAttribute("cart", view(request));
    model.addAttribute("signedIn", OrderWebSupport.optional() != null);
    return "order/cart";
  }

  @GetMapping("/data")
  @ResponseBody
  public Map<String, Object> view(HttpServletRequest request) {
    Actor actor = OrderWebSupport.optional();
    return actor != null ? carts.view(actor) : guests.view(guest(request.getSession()));
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
      result = carts.merge(actor, List.copyOf(g.items), g.mergeKey);
    }
    request.getSession().removeAttribute("utetra.cart");
    return result;
  }
}
