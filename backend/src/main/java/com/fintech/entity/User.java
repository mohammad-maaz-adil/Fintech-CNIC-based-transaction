package com.fintech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Represents a registered user identified by their CNIC.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_cnic", columnList = "cnic", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Pattern(regexp = "\\d{5}-\\d{7}-\\d", message = "CNIC must be in format XXXXX-XXXXXXX-X")
    @Column(nullable = false, unique = true, length = 15)
    public String cnic;

    @NotBlank
    @Column(name = "full_name", nullable = false, length = 100)
    public String fullName;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(nullable = false, length = 20)
    public String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // One user has one account
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public Account account;
}
