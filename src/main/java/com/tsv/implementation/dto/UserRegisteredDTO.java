package com.tsv.implementation.dto;


public class UserRegisteredDTO {


    private String name;



    private String email_id;

    private String password;
    private String faceTemplateBase64;
    private String mobileNumber;
    private String dateOfBirth;
    String role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getEmail_id() {
        return email_id;
    }

    public void setEmail_id(String email_id) {
        this.email_id = email_id;
    }

    public String getPassword() {
        return password;
    }



    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    public String getFaceTemplateBase64() {
        return faceTemplateBase64;
    }

    public void setFaceTemplateBase64(String faceTemplateBase64) {
        this.faceTemplateBase64 = faceTemplateBase64;
    }


}