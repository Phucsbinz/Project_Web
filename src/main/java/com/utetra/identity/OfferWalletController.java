package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/account/offers")
public class OfferWalletController {
 private final OfferWalletService wallet;
 public OfferWalletController(OfferWalletService wallet){this.wallet=wallet;}
 @GetMapping public Object list(@AuthenticationPrincipal UtetraPrincipal p){return wallet.list(p);}
 @PostMapping("/sync") public Object sync(@AuthenticationPrincipal UtetraPrincipal p){return wallet.sync(p);}
 @PostMapping("/{id}/save") public Object save(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id){wallet.save(p,id);return java.util.Map.of("saved",true);}
}
