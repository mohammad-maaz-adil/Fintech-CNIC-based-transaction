package com.fintech.resource;

import com.fintech.entity.Account;
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
import java.util.Map;

/**
 * Profile endpoint - requires JWT.
 *
 * GET /api/profile - returns user profile and account details
 */
@Path("/api/profile")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @Inject
    EntityManager em;

    @GET
    public Response getProfile(@Context SecurityContext ctx) {
        String cnic = ctx.getUserPrincipal().getName();
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.cnic = :cnic", User.class)
                .setParameter("cnic", cnic)
                .getSingleResult();

            Account account = em.createQuery("SELECT a FROM Account a WHERE a.user = :user", Account.class)
                .setParameter("user", user)
                .getSingleResult();

            Map<String, Object> profile = new HashMap<>();
            profile.put("fullName", user.fullName);
            profile.put("cnic", user.cnic);
            profile.put("status", user.status);
            profile.put("accountNumber", account.accountNumber);
            profile.put("balance", account.balance);
            profile.put("currency", account.currency);
            profile.put("createdAt", user.createdAt);

            return Response.ok(profile).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("message", "Profile not found")).build();
        }
    }
}
