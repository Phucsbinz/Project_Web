package com.utetra.controller;
import com.utetra.service.SystemStatusService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class SystemController {
    private final SystemStatusService service;
    public SystemController(SystemStatusService service) { this.service=service; }
    @GetMapping("/api/system/health") public Map<String,String> health() { return service.publicStatus(); }
    @GetMapping("/api/admin/system") public Map<String,String> adminSystem() { return Map.of("scope","ADMIN"); }
}
