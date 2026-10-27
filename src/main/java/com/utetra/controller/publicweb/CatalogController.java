package com.utetra.controller.publicweb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.dto.CatalogRequests.*;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class CatalogController {
    private final CatalogService catalog;
    private final StoreService stores;
    private final PromotionService promotions;
    private final ObjectMapper mapper;
    private final com.utetra.order.service.GroupOrderService groups;
    public CatalogController(CatalogService catalog,StoreService stores,PromotionService promotions,ObjectMapper mapper,com.utetra.order.service.GroupOrderService groups) {
        this.catalog=catalog; this.stores=stores; this.promotions=promotions; this.mapper=mapper;this.groups=groups;
    }
    private Object bean(Object o) { return mapper.convertValue(o,new TypeReference<Object>() {}); }
    @GetMapping("/products/lookups") @ResponseBody public Object lookups() { return Map.of("categories",catalog.categories(false,null)); }
    @GetMapping("/products/csrf") @ResponseBody public Object csrf(org.springframework.security.web.csrf.CsrfToken csrf) {
        return Map.of("headerName",csrf.getHeaderName(),"token",csrf.getToken());
    }
    @GetMapping({"/","/menu"}) public String menu(@RequestParam(defaultValue="") String keyword,
        @RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long storeId,
        @RequestParam(required=false) BigDecimal min,@RequestParam(required=false) BigDecimal max,
        @RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="false") boolean top,@RequestParam(required=false) String group,
        @AuthenticationPrincipal UtetraPrincipal principal,jakarta.servlet.http.HttpServletRequest request,Model model) {
        boolean home="/".equals(request.getServletPath());
        if(group!=null&&!group.isBlank()){
            if(principal==null)return "redirect:/group-orders/"+java.net.URLEncoder.encode(group,java.nio.charset.StandardCharsets.UTF_8);
            var state=groups.view(new com.utetra.order.dto.OrderRequests.Actor(principal.userId(),principal.role()),group);
            if(!Boolean.TRUE.equals(state.get("joined"))||!"OPEN".equals(state.get("status"))||Boolean.TRUE.equals(state.get("expired")))return "redirect:/group-orders/"+group;
            model.addAttribute("groupToken",group);
        }
        model.addAttribute("result",bean(catalog.search(keyword,categoryId,storeId,min,max,home?"sold":sort,page,size,home && principal==null,top)));
        if(home && principal!=null) {
            model.addAttribute("groups",bean(catalog.homeGroups(storeId)));
        }
        model.addAttribute("categories",bean(catalog.categories(false,null))); model.addAttribute("stores",List.of());
        return "catalog/menu";
    }
    @GetMapping("/products/{id}") public String product(@PathVariable Long id,@RequestParam(required=false) String group,@AuthenticationPrincipal UtetraPrincipal principal,Model model) {
        if(group!=null&&!group.isBlank()){
            if(principal==null)return "redirect:/group-orders/"+java.net.URLEncoder.encode(group,java.nio.charset.StandardCharsets.UTF_8);
            var state=groups.view(new com.utetra.order.dto.OrderRequests.Actor(principal.userId(),principal.role()),group);
            if(!Boolean.TRUE.equals(state.get("joined"))||!"OPEN".equals(state.get("status"))||Boolean.TRUE.equals(state.get("expired")))return "redirect:/group-orders/"+group;
            model.addAttribute("groupToken",group);
        }
        model.addAttribute("product",bean(catalog.detail(id))); return "catalog/product";
    }
    @PostMapping("/products/{id}/quote") @ResponseBody public Object quote(@PathVariable Long id,@Valid @RequestBody DrinkInput input) { return catalog.quote(id,input); }
    @GetMapping("/stores") public String stores(Model model) { model.addAttribute("stores",bean(stores.publicStores())); return "catalog/stores"; }
    @GetMapping("/stores/{id}") public String store(@PathVariable Long id,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("store",bean(stores.publicStore(id))); model.addAttribute("promotions",bean(promotions.available(id)));
        model.addAttribute("deliveryRates",bean(stores.deliveryRates(id)));
        model.addAttribute("result",bean(catalog.search("",null,id,null,null,"newest",page,12,false,false))); return "catalog/store";
    }
    @GetMapping("/account/shop-registration") public String registration(@AuthenticationPrincipal UtetraPrincipal p,Model model) {
        model.addAttribute("registrations",bean(stores.registrations(p))); return "catalog/register";
    }
    @PostMapping("/account/shop-registration") @ResponseBody public Object registration(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody StoreInput input) { return stores.register(p,input); }
    @GetMapping({"/vendor","/vendor/{section:products|promotions|stores|shop|areas|stock}","/manager","/manager/{section:products|categories|toppings|promotions|stores|areas|stock}","/admin","/admin/{section:products|categories|toppings|promotions|stores|areas|stock|memberships}"})
    public String management(jakarta.servlet.http.HttpServletRequest request,Model model) {
        String[] path=request.getServletPath().split("/"); model.addAttribute("rolePath",path[1]);
        model.addAttribute("catalogSection",path.length<3 || "shop".equals(path[2]) ? "stores" : path[2]);
        return "catalog/manage";
    }
}
