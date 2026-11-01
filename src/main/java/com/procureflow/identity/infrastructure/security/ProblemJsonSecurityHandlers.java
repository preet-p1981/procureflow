package com.procureflow.identity.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemJsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        write(res, 401, "UNAUTHENTICATED", "Authentication is required");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
            throws IOException {
        write(res, 403, "FORBIDDEN", "You do not have permission to do this");
    }

    private void write(HttpServletResponse res, int status, String code, String detail) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        String correlationId = MDC.get("correlationId");
        res.getWriter().write("""
                {"title":"%s","status":%d,"code":"%s","detail":"%s","correlationId":"%s","timestamp":"%s"}"""
                .formatted(code, status, code, detail, correlationId == null ? "" : correlationId, Instant.now()));
    }
}