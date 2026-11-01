package com.procureflow.identity.infrastructure.security;

import com.procureflow.identity.application.ExternalLoginHandler;
import com.procureflow.identity.application.LoginResult;
import com.procureflow.identity.application.command.ExternalLoginCommand;
import com.procureflow.shared.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ExternalLoginHandler handler;

    GoogleLoginSuccessHandler(ExternalLoginHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException {
        OidcUser google = (OidcUser) auth.getPrincipal();
        try {
            LoginResult r = handler.handle(new ExternalLoginCommand("google", google.getSubject(),
                    google.getEmail(), Boolean.TRUE.equals(google.getEmailVerified()), google.getFullName()));
            endSession(req);
            res.setStatus(200);
            res.setContentType("application/json");
            res.getWriter().write("""
                    {"accessToken":"%s","refreshToken":"%s","tokenType":"Bearer","expiresAt":"%s"}"""
                    .formatted(r.accessToken().value(), r.refreshToken(), r.accessToken().expiresAt()));
        } catch (DomainException e) {
            endSession(req);
            writeError(res, 401, e.errorCode().code());
        }
    }

    static void writeError(HttpServletResponse res, int status, String code) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.getWriter().write("""
                {"status":%d,"code":"%s","detail":"Google login failed"}""".formatted(status, code));
    }

    private static void endSession(HttpServletRequest req) {
        var session = req.getSession(false);
        if (session != null) session.invalidate();
    }
}