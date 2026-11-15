package com.example.university.controller;

import com.example.university.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final UserRepository repository;
    public PageController(UserRepository repository) { this.repository = repository; }

    @GetMapping("/") public String home() { return "redirect:/dashboard"; }
    @GetMapping("/login") public String login() { return "login"; }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        var user = repository.findByEmail(authentication.getName()).orElseThrow();
        model.addAttribute("currentUser", user);
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("displayName", user.getName());
        return "dashboard";
    }
}
