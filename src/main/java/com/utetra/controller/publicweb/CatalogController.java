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
    public CatalogController(CatalogService catalog,StoreService stores,PromotionService promotions,ObjectMapper mapper) {
        this.catalog=catalog; this.stores=stores; this.promotions=promotions; this.mapper=mapper;
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
        @RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="false") boolean top,
        @AuthenticationPrincipal UtetraPrincipal principal,jakarta.servlet.http.HttpServletRequest request,Model model) {
        boolean home="/".equals(request.getServletPath());
        model.addAttribute("result",bean(catalog.search(keyword,categoryId,storeId,min,max,home?"sold":sort,page,size,home && principal==null,top)));
        if(home && principal!=null) {
            Map<String,Object> groups=new LinkedHashMap<>();
            for(String group:List.of("newest","sold","rating","favorites")) groups.put(group,bean(catalog.search("",null,storeId,null,null,group,0,20,false,true)));
            model.addAttribute("groups",groups);
        }
        model.addAttribute("categories",bean(catalog.categories(false,null))); model.addAttribute("stores",bean(stores.publicStores()));
        return "catalog/menu";
    }
    @GetMapping("/products/{id}") public String product(@PathVariable Long id,Model model) {
        model.addAttribute("product",bean(catalog.detail(id))); return "catalog/product";
    }
    @PostMapping("/products/{id}/quote") @ResponseBody public Object quote(@PathVariable Long id,@Valid @RequestBody DrinkInput input) { return catalog.quote(id,input); }
    @GetMapping("/stores") public String stores(Model model) { model.addAttribute("stores",bean(stores.publicStores())); return "catalog/stores"; }
    @GetMapping("/stores/{id}") public String store(@PathVariable Long id,Model model) {
        model.addAttribute("store",bean(stores.publicStore(id))); model.addAttribute("promotions",bean(promotions.available(id)));
        model.addAttribute("deliveryRates",bean(stores.deliveryRates(id)));
        model.addAttribute("result",bean(catalog.search("",null,id,null,null,"newest",0,50,false,false))); return "catalog/store";
    }
    @GetMapping("/account/shop-registration") public String registration() { return "catalog/register"; }
    @PostMapping("/account/shop-registration") @ResponseBody public Object registration(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody StoreInput input) { return stores.register(p,input); }
    @GetMapping({"/vendor","/vendor/{section:products|promotions|stores|shop}","/manager","/manager/{section:products|categories|toppings|promotions|stores}","/admin","/admin/{section:products|categories|toppings|promotions|stores}"})
    public String management(jakarta.servlet.http.HttpServletRequest request,Model model) {
        String role=request.getServletPath().split("/")[1]; model.addAttribute("rolePath",role);
        return "catalog/manage";
    }
}
