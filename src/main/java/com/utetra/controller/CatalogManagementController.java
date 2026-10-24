package com.utetra.controller;

import com.utetra.dto.CatalogRequests.*;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** The three role prefixes share DTOs and services, each service checks current DB scope. */
@RestController
@RequestMapping({"/api/vendor/catalog","/api/manager/catalog","/api/admin/catalog"})
public class CatalogManagementController {
    private final CatalogService catalog;
    private final StoreService stores;
    private final PromotionService promotions;
    private final CatalogMediaService media;
    public CatalogManagementController(CatalogService catalog,StoreService stores,PromotionService promotions,CatalogMediaService media) { this.catalog=catalog; this.stores=stores; this.promotions=promotions; this.media=media; }
    @PostMapping(value="/media/{type}/{id}",consumes="multipart/form-data") public void image(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable String type,@PathVariable Long id,@RequestParam long version,@RequestParam org.springframework.web.multipart.MultipartFile file) { media.upload(p,type,id,version,file); }
    @GetMapping("/stores") public Object stores(@AuthenticationPrincipal UtetraPrincipal p) { return stores.managedStores(p); }
    @GetMapping("/stores/{id}") public Object store(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id) { return stores.managedStore(p,id); }
    @GetMapping("/stores/{id}/commission-policies") public Object policies(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id) { return stores.commissionPolicies(p,id); }
    @PutMapping("/stores/{id}") public Object store(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody StoreInput input) { return stores.update(p,id,input); }
    @PostMapping("/stores/{id}/review") public Object review(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody Decision input) { return stores.review(p,id,input); }
    @PutMapping("/stores/{id}/state") public Object state(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody StoreState input) { return stores.state(p,id,input); }
    @PostMapping("/stores/{id}/memberships") public void assign(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody Membership input,@RequestParam(defaultValue="false") boolean catalogDelegation) { stores.assign(p,id,input,catalogDelegation); }
    @PostMapping("/stores/{id}/areas") public Object area(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody AreaInput input) { return stores.area(p,id,null,input); }
    @PutMapping("/stores/{id}/areas/{areaId}") public Object area(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@PathVariable Long areaId,@Valid @RequestBody AreaInput input) { return stores.area(p,id,areaId,input); }
    @GetMapping("/categories") public Object categories(@AuthenticationPrincipal UtetraPrincipal p) { return catalog.categories(true,p); }
    @PostMapping("/categories") public Object category(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody CategoryInput input) { return catalog.saveCategory(p,null,input); }
    @PutMapping("/categories/{id}") public Object category(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody CategoryInput input) { return catalog.saveCategory(p,id,input); }
    @GetMapping("/toppings") public Object toppings(@AuthenticationPrincipal UtetraPrincipal p) { return catalog.toppings(p); }
    @PostMapping("/toppings") public Object topping(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody ToppingInput input) { return catalog.saveTopping(p,null,input); }
    @PutMapping("/toppings/{id}") public Object topping(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody ToppingInput input) { return catalog.saveTopping(p,id,input); }
    @PutMapping("/stores/{storeId}/toppings/{id}") public void stock(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long storeId,@PathVariable Long id,@RequestBody Availability input) { catalog.availability(p,storeId,id,input.available()); }
    @GetMapping("/products") public Object products(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam Long storeId) { return catalog.managed(p,storeId); }
    @PostMapping("/products") public Object product(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody ProductInput input) { return catalog.saveProduct(p,null,input); }
    @PutMapping("/products/{id}") public Object product(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody ProductInput input) { return catalog.saveProduct(p,id,input); }
    @DeleteMapping("/products/{id}") public void product(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@RequestParam long version) { catalog.disableProduct(p,id,version); }
    @GetMapping("/promotions") public Object promotions(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam(required=false) Long storeId) { return promotions.managed(p,storeId); }
    @PostMapping("/promotions") public Object promotion(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody PromotionInput input) { return promotions.save(p,null,input); }
    @PutMapping("/promotions/{id}") public Object promotion(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable Long id,@Valid @RequestBody PromotionInput input) { return promotions.save(p,id,input); }
}
