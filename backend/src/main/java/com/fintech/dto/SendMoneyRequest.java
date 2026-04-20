package com.fintech.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class SendMoneyRequest {
    @NotBlank(message = "Recipient CNIC is required")
    @Pattern(regexp = "\\d{5}-\\d{7}-\\d", message = "Recipient CNIC must be in format XXXXX-XXXXXXX-X")
    public String recipientCNIC;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least PKR 1.00")
    @DecimalMax(value = "500000.00", message = "Amount cannot exceed PKR 500,000 per transaction")
    public BigDecimal amount;

    @NotBlank(message = "OTP is required")
    public String otp;
}
