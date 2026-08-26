package com.careerpilot.ai.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Runs BEFORE any controller is reached, so @RestControllerAdvice can't
 * catch this. Without it, Spring Security returns its default 401 page
 * (no body / HTML), which is why a missing or expired token can look
 * like a confusing, unexplained failure on the frontend.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String message = "Your session has expired or is invalid. Please log in again.";

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"message\":\"%s\"}",
                Instant.now(),
                message
        );

        response.getWriter().write(json);
    }
}
