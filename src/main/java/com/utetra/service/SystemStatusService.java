package com.utetra.service;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SystemStatusService {
    public Map<String,String> publicStatus() { return Map.of("application","UTETra","status","UP","architecture","Spring MVC / 3-tier"); }
}
