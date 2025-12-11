package com.tsv.implementation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // ✅ Home page route - accessible to everyone
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // ✅ Alternative /home route
    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
}
