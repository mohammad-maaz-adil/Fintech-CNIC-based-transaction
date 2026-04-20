package com.fintech.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpVerifyRequest {
    @NotBlank(message = "OTP is required")
    public String otp;
}
