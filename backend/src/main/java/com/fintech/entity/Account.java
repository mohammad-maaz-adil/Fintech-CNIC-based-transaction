package com.fintech.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bank account linked 1:1 with a User.
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_number", columnList = "account_number", unique = true),
    @Index(name = "idx_accounts_user", columnList = "user_id")
})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    public User user;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    public String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    public String currency = "PKR";

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
