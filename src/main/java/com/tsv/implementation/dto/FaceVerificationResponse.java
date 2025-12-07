package com.tsv.implementation.dto;

public class FaceVerificationResponse {
    private String status;
    private String message;
    private String reason; // From Flask failure reason

    // Constructor, Getters, and Setters
    public FaceVerificationResponse() {}

    public FaceVerificationResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // You may also want one that includes 'reason' for failed cases

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
