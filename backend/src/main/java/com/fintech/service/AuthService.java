package com.fintech.service;

import com.fintech.dto.*;
import com.fintech.entity.Account;
import com.fintech.entity.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles user registration and authentication with CNIC + password.
 * Issues JWTs on successful authentication.
 */
@ApplicationScoped
public class AuthService {

    @jakarta.inject.Inject
    EntityManager em;

    @ConfigProperty(name = "app.jwt.expiry-hours", defaultValue = "24")
    int jwtExpiryHours;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://fintech.local")
    String issuer;

    // Simulation only: use a DB sequence or UUID prefix in production
    private static final AtomicLong accountSeq = new AtomicLong(100001);

    /**
     * Registers a new user. Checks for duplicate CNIC before creating.
     */
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        // Check duplicate CNIC
        long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.cnic = :cnic", Long.class)
            .setParameter("cnic", req.cnic)
            .getSingleResult();
        if (count > 0) {
            throw new IllegalArgumentException("CNIC already registered");
        }

        User user = new User();
        user.cnic = req.cnic;
        user.fullName = req.fullName;
        user.passwordHash = BcryptUtil.bcryptHash(req.password);
        user.status = "ACTIVE";
        em.persist(user);

        // Create linked account with seed balance
        Account account = new Account();
        account.user = user;
        account.accountNumber = "PKF-" + String.format("%08d", accountSeq.getAndIncrement());
        account.balance = new BigDecimal("50000.00"); // seed balance
        account.currency = "PKR";
        em.persist(account);

        String token = generateToken(user);
        return new AuthResponse(token, user.cnic, user.fullName);
    }

    /**
     * Authenticates user by CNIC + password. Returns JWT on success.
     */
    public AuthResponse login(LoginRequest req) {
        User user;
        try {
            user = em.createQuery("SELECT u FROM User u WHERE u.cnic = :cnic", User.class)
                .setParameter("cnic", req.cnic)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityException("Invalid CNIC or password");
        }

        if (!"ACTIVE".equals(user.status)) {
            throw new SecurityException("Account is not active");
        }

        if (!BcryptUtil.matches(req.password, user.passwordHash)) {
            throw new SecurityException("Invalid CNIC or password");
        }

        String token = generateToken(user);
        return new AuthResponse(token, user.cnic, user.fullName);
    }

    private String generateToken(User user) {
        return Jwt.issuer(issuer)
            .subject(user.cnic)
            .claim("userId", user.id)
            .claim("fullName", user.fullName)
            .groups(Set.of("user"))
            .expiresIn(Duration.ofHours(jwtExpiryHours))
            .sign();
    }
}
