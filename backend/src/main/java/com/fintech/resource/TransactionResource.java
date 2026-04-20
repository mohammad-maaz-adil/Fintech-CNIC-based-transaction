package com.fintech.resource;

import com.fintech.dto.SendMoneyRequest;
import com.fintech.dto.ValidateRecipientRequest;
import com.fintech.service.OtpService;
import com.fintech.service.TransactionService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.Map;

/**
 * Transaction endpoints - all require JWT.
 *
 * POST /api/transactions/validate-recipient  - validate recipient CNIC
 * POST /api/transactions/send                - process money transfer (OTP required)
 * GET  /api/transactions/history             - paginated transaction history
 */
@Path("/api/transactions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @Inject
    OtpService otpService;

    @POST
    @Path("/validate-recipient")
    public Response validateRecipient(@Valid ValidateRecipientRequest req, @Context SecurityContext ctx) {
        String senderCNIC = ctx.getUserPrincipal().getName();
        try {
            Map<String, Object> result = transactionService.validateRecipient(req.cnic, senderCNIC);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("message", e.getMessage())).build();
        }
    }

    @POST
    @Path("/send")
    public Response sendMoney(@Valid SendMoneyRequest req, @Context SecurityContext ctx) {
        String senderCNIC = ctx.getUserPrincipal().getName();
        try {
            Map<String, Object> result = transactionService.sendMoney(
                senderCNIC, req.recipientCNIC, req.amount, req.otp);
            return Response.ok(result).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("message", e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("message", "Transaction failed: " + e.getMessage())).build();
        }
    }

    @GET
    @Path("/history")
    public Response getHistory(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size,
        @Context SecurityContext ctx) {
        String cnic = ctx.getUserPrincipal().getName();
        try {
            Map<String, Object> result = transactionService.getHistory(cnic, page, Math.min(size, 100));
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("message", "Failed to load history")).build();
        }
    }
}
