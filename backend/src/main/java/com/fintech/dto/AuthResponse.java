package com.fintech.dto;

public class AuthResponse {
    public String token;
    public String cnic;
    public String fullName;
    public String message;

    public AuthResponse() {}

    public AuthResponse(String token, String cnic, String fullName) {
        this.token = token;
        this.cnic = cnic;
        this.fullName = fullName;
        this.message = "Authentication successful";
    }
}
