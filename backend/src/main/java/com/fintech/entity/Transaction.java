package com.fintech.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a money transfer between two accounts.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_txn_sender", columnList = "sender_account_id"),
    @Index(name = "idx_txn_recipient", columnList = "recipient_account_id"),
    @Index(name = "idx_txn_ref", columnList = "reference_number", unique = true),
    @Index(name = "idx_txn_created", columnList = "created_at")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id", nullable = false)
    public Account senderAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_account_id", nullable = false)
    public Account recipientAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal amount;

    /** PENDING, SUCCESS, FAILED */
    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "reference_number", nullable = false, unique = true, length = 40)
    public String referenceNumber;

    @Column(length = 255)
    public String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
