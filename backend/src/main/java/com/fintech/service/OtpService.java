package com.fintech.service;

import com.fintech.entity.OtpLog;
import com.fintech.entity.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Simulates OTP generation and verification.
 * In production, this would integrate with an SMS gateway.
 */
@ApplicationScoped
public class OtpService {

    @Inject
    EntityManager em;

    @ConfigProperty(name = "app.otp.expiry-minutes", defaultValue = "5")
    int otpExpiryMinutes;

    @ConfigProperty(name = "app.otp.max-attempts", defaultValue = "3")
    int maxAttempts;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a 6-digit OTP for the given user and stores a hash.
     * Returns the plain OTP (for simulation only — real systems send via SMS).
     */
    @Transactional
    public String generateOtp(User user) {
        // Expire old pending OTPs
        em.createQuery("UPDATE OtpLog o SET o.status = 'EXPIRED' WHERE o.user = :user AND o.status = 'PENDING' AND o.expiresAt < :now")
            .setParameter("user", user)
            .setParameter("now", LocalDateTime.now())
            .executeUpdate();

        String otp = String.format("%06d", random.nextInt(1_000_000));

        OtpLog log = new OtpLog();
        log.user = user;
        log.otpCodeHash = BcryptUtil.bcryptHash(otp);
        log.expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        log.status = "PENDING";
        em.persist(log);

        // In production: send via SMS gateway
        // For simulation: log to console
        System.out.printf("[OTP SIM] User %s OTP: %s%n", user.cnic, otp);

        return otp;
    }

    /**
     * Verifies the provided OTP against the latest pending log.
     * Enforces max attempts and expiry.
     */
    @Transactional
    public boolean verifyOtp(User user, String providedOtp) {
        OtpLog log;
        try {
            log = em.createQuery(
                "SELECT o FROM OtpLog o WHERE o.user = :user AND o.status = 'PENDING' ORDER BY o.createdAt DESC",
                OtpLog.class)
                .setParameter("user", user)
                .setMaxResults(1)
                .getSingleResult();
        } catch (NoResultException e) {
            return false;
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(log.expiresAt)) {
            log.status = "EXPIRED";
            return false;
        }

        log.attempts++;

        // Check max attempts
        if (log.attempts > maxAttempts) {
            log.status = "FAILED";
            return false;
        }

        // Verify hash
        if (BcryptUtil.matches(providedOtp, log.otpCodeHash)) {
            log.status = "VERIFIED";
            log.verifiedAt = LocalDateTime.now();
            return true;
        }

        if (log.attempts >= maxAttempts) {
            log.status = "FAILED";
        }
        return false;
    }

    /**
     * Finds user by CNIC (helper).
     */
    public User findUserByCnic(String cnic) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.cnic = :cnic", User.class)
                .setParameter("cnic", cnic)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
