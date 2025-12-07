package com.tsv.implementation.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64; // Required for template image logic

import com.tsv.implementation.service.DefaultUserService;
import com.tsv.implementation.service.FaceRecognitionService; // Use the service for abstraction
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.User;

@RestController
public class FaceVerificationController {


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DefaultUserService userService;

    @Autowired
    private UserRepository userRepo;


    private final String FLASK_VERIFY_URL = "http://127.0.0.1:5000/api/face-verify";


    @PostMapping("/api/face-verify")
    public ResponseEntity<Map<String, Object>> verifyFace(@RequestBody Map<String, String> body, Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status","error","message","not authenticated"));
        }

        String imageBase64 = body.get("image_base64");
        if (imageBase64 == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","image missing"));
        }


        String expectedName = principal.getName();
        User user = userRepo.findByEmail(expectedName);

        if (user == null || user.getFaceImage() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "User or face template not found."));
        }

        String templateBase64 = Base64.getEncoder().encodeToString(user.getFaceImage());


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> flaskRequestBody = Map.of(
                "image_base64", imageBase64,
                "template_base64", templateBase64 // Send the template for 1:1 comparison
        );
        HttpEntity<Map<String,String>> requestEntity = new HttpEntity<>(flaskRequestBody, headers);


        ResponseEntity<Map> flaskResponse;
        try {

            flaskResponse = restTemplate.postForEntity(URI.create(FLASK_VERIFY_URL), requestEntity, Map.class);
        } catch (Exception ex) {
            System.err.println("Error calling Flask service: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status","error","message","failed calling face service", "error", ex.getMessage()));
        }

        Map<String, Object> respBody = flaskResponse.getBody();
        if (respBody == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status","error","message","empty response"));
        }


        String status = (String) respBody.get("status");


        if ("success".equals(status)) {


            user.setActive(true);
            userRepo.save(user);


            UserDetails updatedUserDetails = userService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUserDetails,
                    SecurityContextHolder.getContext().getAuthentication().getCredentials(),
                    updatedUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);


            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("matched", respBody.get("matched")); // This retrieval is now safe because it's going into a HashMap

            return ResponseEntity.ok(responseMap);


        } else {

            return ResponseEntity.ok(Map.of("status", "failed", "reason", respBody.get("reason")));
        }
    }
}