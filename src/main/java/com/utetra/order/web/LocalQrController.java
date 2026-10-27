package com.utetra.order.web;
import com.utetra.order.service.*;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller public class LocalQrController {
 private final LocalQrPaymentService wallet;
 public LocalQrController(LocalQrPaymentService wallet){this.wallet=wallet;}
 @GetMapping("/orders/{id}/qr-payment") public String merchant(@PathVariable long id,Model model){var payment=wallet.merchant(OrderWebSupport.actor(),id);model.addAttribute("payment",payment);model.addAttribute("qrSvg",wallet.qrSvg(payment.get("walletUrl").toString()));return "order/local-qr";}
 @GetMapping("/orders/{id}/qr-payment/status") @ResponseBody public Object status(@PathVariable long id){var value=new java.util.LinkedHashMap<>(wallet.merchant(OrderWebSupport.actor(),id));value.remove("walletUrl");return value;}
 @PostMapping("/orders/{id}/qr-payment/renew") @ResponseBody public Object renew(@PathVariable long id){return wallet.renew(OrderWebSupport.actor(),id);}
 @GetMapping("/local-wallet/pay/{id}") public String phone(@PathVariable String id,Model model){if(!id.matches("[0-9a-fA-F-]{36}"))throw OrderProblem.missing();model.addAttribute("publicId",id);return "order/local-wallet";}
 public record WalletInput(String token,BigDecimal expectedAmount,boolean acknowledged){}
 @PostMapping("/local-wallet/api/{id}/status") @ResponseBody public Object publicStatus(@PathVariable String id,@RequestBody WalletInput input){return wallet.publicStatus(id,input.token());}
 @PostMapping("/local-wallet/api/{id}/confirm") @ResponseBody public Object confirm(@PathVariable String id,@RequestBody WalletInput input){return wallet.confirm(id,input.token(),input.expectedAmount(),input.acknowledged());}
}
