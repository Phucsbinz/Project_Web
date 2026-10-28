package com.utetra.order.web;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.service.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OperationsController {
  private final OperationsService operations;

  public OperationsController(OperationsService operations) {
    this.operations = operations;
  }

  @GetMapping({"/admin/order-tools", "/manager/order-tools"})
  public String tools(Model model) {
    var actor = OrderWebSupport.actor();
    model.addAttribute("carriers", operations.carriers(actor));
    model.addAttribute("stores", operations.stores(actor));
    model.addAttribute("areas", operations.deliveryAreas(actor));
    model.addAttribute("rates", operations.deliveryRates(actor));
    model.addAttribute("admin", actor.is("ADMIN"));
    return "order/tools";
  }

  @PostMapping({"/admin/order-tools/carriers", "/manager/order-tools/carriers"})
  @ResponseBody
  public Map<String, Object> carrier(@Valid @RequestBody CarrierInput request) {
    return OrderWebSupport.carrier(operations.carrier(OrderWebSupport.actor(), request));
  }

  @PostMapping({"/admin/order-tools/rates", "/manager/order-tools/rates"})
  @ResponseBody
  public Map<String, Object> rate(@Valid @RequestBody RateInput request) {
    var r = operations.rate(OrderWebSupport.actor(), request);
    return Map.of("id", r.id, "fee", r.additionalFee);
  }

  @PostMapping({"/admin/order-tools/commissions", "/manager/order-tools/commissions"})
  @ResponseBody
  public Map<String, Object> commission(@Valid @RequestBody CommissionInput request) {
    var c = operations.commission(OrderWebSupport.actor(), request);
    return Map.of("id", c.id, "rate", c.ratePercent);
  }

  @PostMapping("/admin/order-tools/commissions/global")
  @ResponseBody
  public Map<String, Object> globalCommission(@Valid @RequestBody GlobalCommissionInput request) {
    return Map.of(
        "storesUpdated", operations.globalCommission(OrderWebSupport.actor(), request.percent()));
  }

  @GetMapping({"/vendor/revenue", "/manager/reports", "/admin/reports"})
  public String report(
      @RequestParam(required = false) Long store,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      Model model) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    if(OrderWebSupport.actor().is("MANAGER")) {
      model.addAttribute("report",operations.operationalReport(OrderWebSupport.actor(),store,from==null?today.minusDays(6):from,to==null?today:to));
      model.addAttribute("stores",operations.stores(OrderWebSupport.actor()));
      return "order/operational-report";
    }
    model.addAttribute(
        "report",
        operations.revenue(
            OrderWebSupport.actor(),
            store,
            from == null ? today.minusDays(6) : from,
            to == null ? today : to));
    model.addAttribute("stores", operations.stores(OrderWebSupport.actor()));
    return "order/report";
  }

  @GetMapping({"/vendor/revenue.csv", "/manager/reports.csv", "/admin/reports.csv"})
  @ResponseBody
  public ResponseEntity<String> export(
      @RequestParam(required = false) Long store,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to) {
    var r = operations.revenue(OrderWebSupport.actor(), store, from, to);
    String csv =
        "from,to,orders,goods,shipping,commission,refund,commission_reversed,store_net\n"
            + String.join(
                ",",
                List.of(
                        "from",
                        "to",
                        "orders",
                        "goods",
                        "shipping",
                        "commission",
                        "refund",
                        "commissionReversed",
                        "storeNet")
                    .stream()
                    .map(k -> r.get(k).toString())
                    .toList())
            + "\n";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=utetra-revenue.csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(csv);
  }

  @GetMapping("/notifications")
  public String notifications(@RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute("notifications", operations.notifications(OrderWebSupport.actor(), page));
    return "order/notifications";
  }

  @GetMapping("/notifications/data")
  @ResponseBody
  public List<Map<String, Object>> notificationsData(@RequestParam(defaultValue = "0") int page) {
    return operations.notifications(OrderWebSupport.actor(), page);
  }

  @PostMapping("/notifications/{id}/read")
  @ResponseBody
  public Map<String, Boolean> read(@PathVariable long id) {
    operations.readNotification(OrderWebSupport.actor(), id);
    return Map.of("read", true);
  }

  @GetMapping("/shipper/reports")
  public String shipperReport(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      Model model) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    model.addAttribute(
        "report",
        operations.shipperReport(
            OrderWebSupport.actor(),
            from == null ? today.minusDays(6) : from,
            to == null ? today : to));
    return "order/shipper-report";
  }
}
