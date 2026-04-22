package com.fintech.config;

import com.fintech.entity.Account;
import com.fintech.entity.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import io.quarkus.arc.profile.IfBuildProfile;

@IfBuildProfile("dev")
@ApplicationScoped
public class DevDataSeeder {

    @Inject
    EntityManager em;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        seedUser("42101-1234567-1", "Mohammad Maaz Adil", "password123", new BigDecimal("75000.00"), "PKF-00100001");
        seedUser("35202-7654321-2", "Ayesha Siddiqui", "password123", new BigDecimal("32500.50"), "PKF-00100002");
        seedUser("42301-9876543-3", "Ali Hassan Malik", "password123", new BigDecimal("120000.00"), "PKF-00100003");
        seedUser("31202-1111111-4", "Fatima Zahra", "password123", new BigDecimal("15000.75"), "PKF-00100004");
    }

    private void seedUser(String cnic, String fullName, String rawPassword, BigDecimal balance, String accNo) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.cnic = :cnic", Long.class)
                .setParameter("cnic", cnic)
                .getSingleResult();
        if (count > 0) return;

        User u = new User();
        u.cnic = cnic;
        u.fullName = fullName;
        u.passwordHash = BcryptUtil.bcryptHash(rawPassword);
        u.status = "ACTIVE";
        em.persist(u);

        Account a = new Account();
        a.user = u;
        a.accountNumber = accNo;
        a.balance = balance;
        a.currency = "PKR";
        em.persist(a);
    }
}