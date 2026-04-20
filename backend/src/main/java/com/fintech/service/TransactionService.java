package com.fintech.service;

import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import com.fintech.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core transaction processing service.
 * All money transfers go through this service to ensure transactional integrity.
 */
@ApplicationScoped
public class TransactionService {

    @Inject
    EntityManager em;

    @Inject
    OtpService otpService;

    /**
     * Validates that a recipient CNIC exists and has an active account.
     * Returns recipient details on success.
     */
    public Map<String, Object> validateRecipient(String recipientCNIC, String senderCNIC) {
        if (recipientCNIC.equals(senderCNIC)) {
            throw new IllegalArgumentException("Cannot send money to yourself");
        }

        User recipient;
        try {
            recipient = em.createQuery(
                "SELECT u FROM User u WHERE u.cnic = :cnic AND u.status = 'ACTIVE'", User.class)
                .setParameter("cnic", recipientCNIC)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Recipient not found or account is not active");
        }

        Account account;
        try {
            account = em.createQuery(
                "SELECT a FROM Account a WHERE a.user = :user", Account.class)
                .setParameter("user", recipient)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Recipient does not have an active bank account");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("recipientName", recipient.fullName);
        result.put("accountId", account.id);
        result.put("accountNumber", account.accountNumber);
        return result;
    }

    /**
     * Processes a money transfer with full transactional integrity.
     * Validates: OTP, sender account, balance, recipient account.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public Map<String, Object> sendMoney(String senderCNIC, String recipientCNIC, BigDecimal amount, String otp) {
        // Find sender
        User sender = findUserByCnic(senderCNIC);
        if (sender == null) throw new SecurityException("Sender not found");

        // Verify OTP first
        if (!otpService.verifyOtp(sender, otp)) {
            throw new SecurityException("Invalid or expired OTP");
        }

        // Get sender account with pessimistic write lock to prevent concurrent transfers
        Account senderAccount = em.createQuery(
            "SELECT a FROM Account a WHERE a.user = :user", Account.class)
            .setParameter("user", sender)
            .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();

        // Validate balance
        if (senderAccount.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient balance. Available: PKR %.2f", senderAccount.balance)
            );
        }

        // Find recipient
        User recipient;
        try {
            recipient = em.createQuery(
                "SELECT u FROM User u WHERE u.cnic = :cnic AND u.status = 'ACTIVE'", User.class)
                .setParameter("cnic", recipientCNIC)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Recipient account not found");
        }

        Account recipientAccount = em.createQuery(
            "SELECT a FROM Account a WHERE a.user = :user", Account.class)
            .setParameter("user", recipient)
            .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();

        // Debit sender
        senderAccount.balance = senderAccount.balance.subtract(amount);

        // Credit recipient
        recipientAccount.balance = recipientAccount.balance.add(amount);

        // Create transaction record
        String ref = "TXN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction txn = new Transaction();
        txn.senderAccount = senderAccount;
        txn.recipientAccount = recipientAccount;
        txn.amount = amount;
        txn.status = "SUCCESS";
        txn.referenceNumber = ref;
        txn.description = String.format("Transfer to %s", recipient.fullName);
        em.persist(txn);

        Map<String, Object> result = new HashMap<>();
        result.put("referenceNumber", ref);
        result.put("amount", amount);
        result.put("recipientName", recipient.fullName);
        result.put("newBalance", senderAccount.balance);
        result.put("status", "SUCCESS");
        return result;
    }

    /**
     * Returns paginated transaction history for a user.
     */
    public Map<String, Object> getHistory(String cnic, int page, int size) {
        User user = findUserByCnic(cnic);
        if (user == null) throw new SecurityException("User not found");

        Account account;
        try {
            account = em.createQuery("SELECT a FROM Account a WHERE a.user = :user", Account.class)
                .setParameter("user", user)
                .getSingleResult();
        } catch (NoResultException e) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("transactions", List.of());
            empty.put("totalPages", 0);
            return empty;
        }

        long total = em.createQuery(
            "SELECT COUNT(t) FROM Transaction t WHERE t.senderAccount = :acc OR t.recipientAccount = :acc",
            Long.class)
            .setParameter("acc", account)
            .getSingleResult();

        List<Transaction> txns = em.createQuery(
            "SELECT t FROM Transaction t WHERE t.senderAccount = :acc OR t.recipientAccount = :acc ORDER BY t.createdAt DESC",
            Transaction.class)
            .setParameter("acc", account)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();

        List<Map<String, Object>> txnList = txns.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.id);
            m.put("amount", t.amount);
            m.put("status", t.status);
            m.put("referenceNumber", t.referenceNumber);
            m.put("description", t.description);
            m.put("createdAt", t.createdAt);
            boolean isSender = t.senderAccount.id.equals(account.id);
            m.put("type", isSender ? "debit" : "credit");
            return m;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("transactions", txnList);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }

    private User findUserByCnic(String cnic) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.cnic = :cnic", User.class)
                .setParameter("cnic", cnic)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
