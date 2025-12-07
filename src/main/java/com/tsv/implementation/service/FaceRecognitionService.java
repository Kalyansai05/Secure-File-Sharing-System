package com.tsv.implementation.service;

import com.tsv.implementation.dto.FaceVerificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
public class FaceRecognitionService {

    // IMPORTANT: Define the URL for your Flask service
    @Value("${face.recognition.url:http://localhost:5000/api/face-verify}")
    private String flaskApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Calls the Flask service to compare the live image against the template.
     *
     * @param liveImageBase64     The base64 string of the image captured by the user.
     * @param templateImageBase64 The base64 string of the user's stored template image.
     * @return FaceVerificationResponse object.
     */
    public ResponseEntity<FaceVerificationResponse> verify(String liveImageBase64, String templateImageBase64) {
        ResponseEntity<FaceVerificationResponse> responseEntity = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Body contains the live image and the user's template for a 1:1 comparison
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("image_base64", liveImageBase64);
        requestBody.put("template_base64", templateImageBase64); // Send template to Python

        try {
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Call the Python service
            ResponseEntity<FaceVerificationResponse> response = restTemplate.postForEntity(
                    flaskApiUrl,
                    entity,
                    FaceVerificationResponse.class
            );


        } catch (Exception e) {
            System.err.println("Error calling Flask service: " + e.getMessage());
            FaceVerificationResponse errorResponse = new FaceVerificationResponse("error", "Backend verification service unavailable or failed.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }   return responseEntity;
    }
}