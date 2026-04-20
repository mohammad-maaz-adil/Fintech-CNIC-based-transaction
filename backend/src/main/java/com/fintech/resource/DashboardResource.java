package com.fintech.resource;

import com.fintech.entity.Account;
import com.fintech.entity.Transaction;
import com.fintech.entity.User;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard summary endpoint - requires JWT.
 *
 * GET /api/dashboard/summary - returns balance, account info, recent transactions
 */
@Path("/api/dashboard")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/summary")
    public Response getSummary(@Context SecurityContext ctx) {
        String cnic = ctx.getUserPrincipal().getName();
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.cnic = :cnic", User.class)
                .setParameter("cnic", cnic)
                .getSingleResult();

            Account account = em.createQuery("SELECT a FROM Account a WHERE a.user = :user", Account.class)
                .setParameter("user", user)
                .getSingleResult();

            List<Transaction> recent = em.createQuery(
                "SELECT t FROM Transaction t WHERE t.senderAccount = :acc OR t.recipientAccount = :acc ORDER BY t.createdAt DESC",
                Transaction.class)
                .setParameter("acc", account)
                .setMaxResults(5)
                .getResultList();

            List<Map<String, Object>> recentList = recent.stream().map(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.id);
                m.put("amount", t.amount);
                m.put("description", t.description);
                m.put("createdAt", t.createdAt);
                boolean isSender = t.senderAccount.id.equals(account.id);
                m.put("type", isSender ? "debit" : "credit");
                return m;
            }).toList();

            Map<String, Object> summary = new HashMap<>();
            summary.put("fullName", user.fullName);
            summary.put("cnic", user.cnic);
            summary.put("accountNumber", account.accountNumber);
            summary.put("balance", account.balance);
            summary.put("currency", account.currency);
            summary.put("recentTransactions", recentList);

            return Response.ok(summary).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("message", "Account not found")).build();
        }
    }
}
