package com.utetra.support;
import com.utetra.security.UtetraPrincipal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.utetra.identity.StrictNumbers;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.*;
import java.util.Map;
@Controller
public class SupportController {
 private final SupportService support;
 public SupportController(SupportService support){this.support=support;}
 public record Start(@JsonDeserialize(using=StrictNumbers.LongValue.class) Long storeId){}
 public record Send(String body,String clientMessageId){}
 @GetMapping({"/support","/manager/support","/admin/support"})
 public String page(@AuthenticationPrincipal UtetraPrincipal p,Model model,HttpServletRequest request,HttpServletResponse response){support.require(p);String expected=p.role().equals("ROLE_USER")?"/support":p.role().equals("ROLE_MANAGER")?"/manager/support":"/admin/support";if(!request.getServletPath().equals(expected))throw SupportProblem.denied();model.addAttribute("supportUserId",p.userId());model.addAttribute("supportCustomer",p.role().equals("ROLE_USER"));response.setHeader("Cache-Control","no-store");return "support/chat";}
 @GetMapping("/api/support/targets") @ResponseBody public Map<String,Object> targets(@AuthenticationPrincipal UtetraPrincipal p){return support.targets(p);}
 @GetMapping("/api/support/conversations") @ResponseBody public Map<String,Object> list(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam(defaultValue="0") int page){return support.list(p,page);}
 @PostMapping("/api/support/conversations") @ResponseBody public Map<String,Object> open(@AuthenticationPrincipal UtetraPrincipal p,@RequestBody Start input){return support.open(p,input.storeId());}
 @GetMapping("/api/support/conversations/{id}/messages") @ResponseBody public Map<String,Object> messages(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable String id,@RequestParam(required=false) Long before,@RequestParam(required=false) Long after){return support.messages(p,id,before,after);}
 @PostMapping("/api/support/conversations/{id}/messages") @ResponseBody public Map<String,Object> send(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable String id,@RequestBody Send input){return support.send(p,id,input.body(),input.clientMessageId());}
}
