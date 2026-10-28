package com.utetra.order.web;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping({
  "/orders",
  "/vendor/orders",
  "/manager/orders",
  "/admin/orders",
  "/shipper/orders"
})
public class OrderController {
  private final OrderWorkflowService orders;
  private final ReturnEvidenceService evidence;
  private final BankTransferService bankTransfers;
  private final BankTransferSettings bank;

  public OrderController(OrderWorkflowService orders, ReturnEvidenceService evidence,
      BankTransferService bankTransfers, BankTransferSettings bank) {
    this.orders = orders;
    this.evidence = evidence;
    this.bankTransfers = bankTransfers;
    this.bank = bank;
  }

  private String base(HttpServletRequest r) {
    String path = r.getRequestURI().substring(r.getContextPath().length());
    int index = path.indexOf("/orders");
    return path.substring(0, index + 7);
  }

  private String mode(HttpServletRequest r) {
    String b = base(r);
    return b.equals("/orders") ? "buyer" : b.startsWith("/shipper") ? "shipper" : "operator";
  }

  @GetMapping
  public String list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      HttpServletRequest request,
      Model model) {
    model.addAttribute(
        "orders", orders.list(OrderWebSupport.actor(), mode(request), status, page, 20));
    model.addAttribute("base", base(request));
    model.addAttribute("page", page);
    model.addAttribute("status", status == null ? "" : status);
    return "order/list";
  }

  @GetMapping("/data")
  @ResponseBody
  public List<Map<String, Object>> listData(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      HttpServletRequest request) {
    return orders.list(OrderWebSupport.actor(), mode(request), status, page, 20);
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable long id, HttpServletRequest request, Model model) {
    model.addAttribute("order", orders.read(OrderWebSupport.actor(), id));
    model.addAttribute("base", base(request));
    model.addAttribute("mode", mode(request));
    model.addAttribute("role", OrderWebSupport.actor().role());
    model.addAttribute("bank", bank.instructions(id));
    return "order/detail";
  }

  @PostMapping("/{id}/received") @ResponseBody
  public Map<String,Object> received(@PathVariable long id){return orders.confirmReceived(OrderWebSupport.actor(),id);}

  @GetMapping("/{id}/data")
  @ResponseBody
  public Map<String, Object> detailData(@PathVariable long id) {
    return orders.read(OrderWebSupport.actor(), id);
  }

  @PostMapping("/{id}/status")
  @ResponseBody
  public Map<String, Object> status(@PathVariable long id, @Valid @RequestBody Transition request) {
    return orders.transition(OrderWebSupport.actor(), id, request);
  }

  @PostMapping("/{id}/bank-confirmation")
  @ResponseBody
  public Map<String, Object> confirmBank(@PathVariable long id, @Valid @RequestBody BankConfirmation request) {
    return bankTransfers.confirm(OrderWebSupport.actor(), id, request);
  }

  @PostMapping("/{id}/cancel")
  @ResponseBody
  public Map<String, Object> cancel(@PathVariable long id, @Valid @RequestBody Transition request) {
    return orders.cancel(OrderWebSupport.actor(), id, request.version(), request.reason());
  }

  @PostMapping("/{id}/returns")
  @ResponseBody
  public Map<String, Object> requestReturn(
      @PathVariable long id, @Valid @RequestBody ReturnInput request) {
    return orders.requestReturn(OrderWebSupport.actor(), id, request.reason());
  }

  @PostMapping(value = "/{id}/returns/evidence", consumes = "multipart/form-data")
  @ResponseBody
  public Map<String, Object> returnWithEvidence(
      @PathVariable long id,
      @RequestParam String reason,
      @RequestParam(required = false) org.springframework.web.multipart.MultipartFile image) {
    return evidence.submit(OrderWebSupport.actor(), id, reason, image);
  }

  @PostMapping("/{id}/returns/review")
  @ResponseBody
  public Map<String, Object> review(@PathVariable long id, @Valid @RequestBody Decision request) {
    return orders.reviewReturn(OrderWebSupport.actor(), id, request);
  }

  @PostMapping("/{id}/returns/receive")
  @ResponseBody
  public Map<String, Object> receive(
      @PathVariable long id, @Valid @RequestBody VersionInput request) {
    return orders.receiveReturn(OrderWebSupport.actor(), id, request.version());
  }

  @PostMapping("/{id}/refund")
  @ResponseBody
  public Map<String, Object> refund(
      @PathVariable long id, @Valid @RequestBody RefundInput request) {
    return orders.refundCash(OrderWebSupport.actor(), id, request);
  }

  @PostMapping("/{id}/failure")
  @ResponseBody
  public Map<String, Object> failure(
      @PathVariable long id, @Valid @RequestBody Transition request) {
    return orders.deliveryFailure(OrderWebSupport.actor(), id, request.version(), request.reason());
  }

  @PostMapping("/{id}/assignment")
  @ResponseBody
  public Map<String, Object> assignment(
      @PathVariable long id, @Valid @RequestBody Assignment request) {
    orders.assign(OrderWebSupport.actor(), id, request);
    return orders.read(OrderWebSupport.actor(), id);
  }
}
