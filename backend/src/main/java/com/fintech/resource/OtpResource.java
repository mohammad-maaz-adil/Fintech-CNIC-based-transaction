package com.fintech.resource;

import com.fintech.dto.OtpRequest;
import com.fintech.dto.OtpVerifyRequest;
import com.fintech.entity.User;
import com.fintech.service.OtpService;
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
 * OTP simulation endpoints - require JWT.
 *
 * POST /api/otp/send    - generate and "send" OTP (returns OTP in response for simulation)
 * POST /api/otp/verify  - verify provided OTP
 */
@Path("/api/otp")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OtpResource {

    @Inject
    OtpService otpService;

    @POST
    @Path("/send")
    public Response sendOtp(OtpRequest req, @Context SecurityContext ctx) {
        String cnic = ctx.getUserPrincipal().getName();
        User user = otpService.findUserByCnic(cnic);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("message", "User not found")).build();
        }
        try {
            String otp = otpService.generateOtp(user);
            // SIMULATION ONLY: in production never return the OTP in the response
            return Response.ok(Map.of(
                "message", "OTP sent successfully",
                "otp", otp,  // for simulation/testing only
                "expiresInMinutes", 5
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("message", "Failed to send OTP")).build();
        }
    }

    @POST
    @Path("/verify")
    public Response verifyOtp(@Valid OtpVerifyRequest req, @Context SecurityContext ctx) {
        String cnic = ctx.getUserPrincipal().getName();
        User user = otpService.findUserByCnic(cnic);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("message", "User not found")).build();
        }
        boolean valid = otpService.verifyOtp(user, req.otp);
        if (valid) {
            return Response.ok(Map.of("valid", true, "message", "OTP verified successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("valid", false, "message", "Invalid, expired, or exceeded OTP")).build();
        }
    }
}
