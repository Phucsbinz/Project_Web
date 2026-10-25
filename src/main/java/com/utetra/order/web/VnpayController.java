package com.utetra.order.web;

import com.utetra.order.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Controller
public class VnpayController {
  private final VnpayService payments;
  public VnpayController(VnpayService payments) {this.payments=payments;}
  @PostMapping("/orders/{id}/vnpay") @ResponseBody
  public Map<String,String> start(@PathVariable long id,HttpServletRequest request) {
    return Map.of("url",payments.start(OrderWebSupport.actor(),id,request.getRemoteAddr()));
  }
  @GetMapping("/payments/vnpay/ipn") @ResponseBody
  public Map<String,String> ipn(@RequestParam MultiValueMap<String,String> params) {
    if(params.size()>50 || params.values().stream().anyMatch(v->v.size()!=1 || v.getFirst().length()>1000))
      return Map.of("RspCode","99","Message","Invalid request");
    try {return payments.ipn(params.toSingleValueMap());}
    catch(org.springframework.dao.DataAccessException e) {return Map.of("RspCode","99","Message","Retry later");}
  }
  @GetMapping("/payments/vnpay/return")
  public String returned() {return "redirect:/orders";} // Never mark PAID from the browser redirect.
}
