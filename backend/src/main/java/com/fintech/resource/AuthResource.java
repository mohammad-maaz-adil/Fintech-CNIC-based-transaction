package com.fintech.resource;

import com.fintech.dto.AuthResponse;
import com.fintech.dto.LoginRequest;
import com.fintech.dto.SignupRequest;
import com.fintech.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest req) {
        try {
            AuthResponse res = authService.signup(req);
            return Response.status(Response.Status.CREATED).entity(res).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody(e.getMessage()))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorBody("Registration failed: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest req) {
        try {
            AuthResponse res = authService.login(req);
            return Response.ok(res).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorBody(e.getMessage()))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorBody("Login failed: " + e.getMessage()))
                    .build();
        }
    }

    private java.util.Map<String, String> errorBody(String message) {
        return java.util.Map.of("message", message);
    }
}