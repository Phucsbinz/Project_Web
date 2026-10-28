package com.utetra.order.web;
import com.utetra.order.service.ShipperScopeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class AdminShipperScopeController {
 private final ShipperScopeService scopes;
 public AdminShipperScopeController(ShipperScopeService scopes){this.scopes=scopes;}
 @GetMapping("/admin/shipper-scopes") public String page(Model model){scopes.choices(OrderWebSupport.actor()).forEach(model::addAttribute);return "order/shipper-scopes";}
 public record ScopeInput(@Positive long shipperId,@Positive long storeId,@NotNull Boolean active,@NotNull @Min(0) Long version,@NotBlank @Size(max=500) String reason){}
 @PostMapping("/admin/shipper-scopes") @ResponseBody public Map<String,Boolean> set(@Valid @RequestBody ScopeInput input){scopes.set(OrderWebSupport.actor(),input.shipperId(),input.storeId(),input.active(),input.version(),input.reason());return Map.of("saved",true);}
}
