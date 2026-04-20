package com.fintech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ValidateRecipientRequest {
    @NotBlank(message = "CNIC is required")
    @Pattern(regexp = "\\d{5}-\\d{7}-\\d", message = "CNIC must be in format XXXXX-XXXXXXX-X")
    public String cnic;
}
