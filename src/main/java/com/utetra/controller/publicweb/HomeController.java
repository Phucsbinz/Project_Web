package com.utetra.controller.publicweb;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/") public String home(Model model) {
        model.addAttribute("stack",List.of("Spring Boot","JSP/JSTL","SiteMesh","Bootstrap","JPA / MySQL","JWT","WebSocket","Cloudinary"));
        return "publicweb/home";
    }
    @GetMapping("/auth/login") public String login() { return "auth/login"; }
}
