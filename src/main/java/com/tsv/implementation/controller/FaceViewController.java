package com.tsv.implementation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FaceViewController {

    // This handles the HTTP GET request and returns the Thymeleaf template name.
    // This is the clean way to render your face-check.html page.
    @GetMapping("/face-check")
    public String faceCheckPage() {
        return "face-check"; // Maps to src/main/resources/templates/face-check.html
    }
}