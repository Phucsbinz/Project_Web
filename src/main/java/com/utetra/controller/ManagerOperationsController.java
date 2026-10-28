package com.utetra.controller;

import com.utetra.security.UtetraPrincipal;
import com.utetra.service.ManagerOperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ManagerOperationsController {
    private final ManagerOperationsService operations;
    public ManagerOperationsController(ManagerOperationsService operations) {this.operations=operations;}
    public record Accepting(@NotNull Boolean acceptingOrders,@NotNull @PositiveOrZero Long version) {}
    public record Availability(@NotNull Boolean available,@NotNull @PositiveOrZero Long version) {}
    public record Stock(@NotNull @Min(0) @Max(1000000) @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.IntValue.class) Integer quantity,@NotNull @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {}
    @GetMapping("/api/manager/operations/stores") public Object stores(@AuthenticationPrincipal UtetraPrincipal p) {return operations.stores(p);}
    @PutMapping("/api/manager/operations/stores/{id}/accepting") public Object accepting(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody Accepting input) {return operations.accepting(p,id,input.acceptingOrders(),input.version());}
    @PutMapping("/api/manager/operations/products/{id}/availability") public Object available(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody Availability input) {return operations.availability(p,id,input.available(),input.version());}
    @PutMapping("/api/manager/operations/variants/{id}/stock") public Object stock(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody Stock input) {return operations.stock(p,id,input.quantity(),input.version());}
    @GetMapping("/api/admin/operations/managers") public Object managers(@AuthenticationPrincipal UtetraPrincipal p) {return operations.managers(p);}
    @GetMapping("/api/vendor/operations/stores/{id}/managers") public Object assigned(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id) {return operations.assignedManagers(p,id);}
}
