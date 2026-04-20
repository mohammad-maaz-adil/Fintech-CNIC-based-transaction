package com.fintech.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores OTP events for audit and verification.
 */
@Entity
@Table(name = "otp_logs", indexes = {
    @Index(name = "idx_otp_user", columnList = "user_id"),
    @Index(name = "idx_otp_expires", columnList = "expires_at")
})
public class OtpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    /** Hashed OTP code - never store plain text */
    @Column(name = "otp_code_hash", nullable = false, length = 64)
    public String otpCodeHash;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "attempts", nullable = false)
    public int attempts = 0;

    @Column(name = "verified_at")
    public LocalDateTime verifiedAt;

    /** PENDING, VERIFIED, EXPIRED, FAILED */
    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
