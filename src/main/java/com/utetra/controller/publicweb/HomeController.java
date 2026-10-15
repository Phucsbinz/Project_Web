package com.utetra.controller.publicweb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/auth/login") public String login() { return "auth/login"; }
}
