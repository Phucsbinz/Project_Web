package com.utetra.order.web;
import com.utetra.order.service.*;
import com.utetra.order.dto.OrderRequests.VersionInput;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/group-orders")
public class GroupOrderController {
 private final GroupOrderService groups;
 @GetMapping("/lifecycle-notices") @ResponseBody public Object lifecycleNotices(){return groups.lifecycleNotices(OrderWebSupport.actor());}
 @GetMapping("/payment-notices") @ResponseBody public Object paymentNotices(){return groups.paymentNotices(OrderWebSupport.actor());}
 public GroupOrderController(GroupOrderService groups){this.groups=groups;}
 public record Create(@jakarta.validation.constraints.Pattern(regexp="HOST|INDIVIDUAL") String paymentMode){}
 @PostMapping @ResponseBody public Object create(@Valid @RequestBody(required=false) Create input){return groups.create(OrderWebSupport.actor(),input==null||input.paymentMode()==null?"HOST":input.paymentMode());}
 @GetMapping("/new") public String start(Model model){model.addAttribute("groups",groups.mine(OrderWebSupport.actor()));return "order/group-start";}
 @GetMapping("/{token}") public String page(@PathVariable String token,Model model){var group=groups.view(OrderWebSupport.actor(),token);if(Set.of("CANCELLED","EXPIRED").contains(group.get("status")))return "redirect:/group-orders/new";model.addAttribute("group",group);return "order/group";}
 @GetMapping("/{token}/payment") public String paymentPage(@PathVariable String token,Model model){var payment=groups.paymentPage(OrderWebSupport.actor(),token);if("LOCAL_QR".equals(payment.get("paymentMethod"))&&Boolean.TRUE.equals(payment.get("payer")))return "redirect:/orders/"+payment.get("orderId")+"/qr-payment";model.addAttribute("payment",payment);return "order/group-payment";}
 @GetMapping("/{token}/data") @ResponseBody public Object view(@PathVariable String token){return groups.view(OrderWebSupport.actor(),token);}
 @PostMapping("/{token}/{action:join|leave|lock|cancel|confirm}") @ResponseBody public Object action(@PathVariable String token,@PathVariable String action,@Valid @RequestBody VersionInput input){var actor=OrderWebSupport.actor();return switch(action){case "join"->groups.join(actor,token,input.version());case "leave"->groups.leave(actor,token,input.version());case "confirm"->groups.confirm(actor,token,input.version());case "lock"->groups.lock(actor,token,input.version());default->groups.cancel(actor,token,input.version());};}
 public record AddItem(@jakarta.validation.constraints.PositiveOrZero long version,@jakarta.validation.constraints.NotNull @Valid com.utetra.order.dto.OrderRequests.Item item){}
 public record RemoveItem(@jakarta.validation.constraints.PositiveOrZero long version,@jakarta.validation.constraints.Pattern(regexp="[0-9a-f]{64}") String configuration){}
 @PostMapping("/{token}/items") @ResponseBody public Object add(@PathVariable String token,@Valid @RequestBody AddItem input){return groups.addItem(OrderWebSupport.actor(),token,input.version(),input.item());}
 @PostMapping("/{token}/items/remove") @ResponseBody public Object remove(@PathVariable String token,@Valid @RequestBody RemoveItem input){return groups.removeItem(OrderWebSupport.actor(),token,input.version(),input.configuration());}

 public record PaymentMode(@jakarta.validation.constraints.PositiveOrZero long version,@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="HOST|INDIVIDUAL") String paymentMode){}
 @PostMapping("/{token}/payment-mode") @ResponseBody public Object paymentMode(@PathVariable String token,@Valid @RequestBody PaymentMode input){return groups.paymentMode(OrderWebSupport.actor(),token,input.version(),input.paymentMode());}

}
