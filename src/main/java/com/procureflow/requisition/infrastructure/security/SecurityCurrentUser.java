package com.procureflow.requisition.infrastructure.security;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.requisition.application.port.CurrentUser;
import com.procureflow.shared.domain.DomainException;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityCurrentUser implements CurrentUser {

    private final UserRepository users;

    SecurityCurrentUser(UserRepository users) {
        this.users = users;
    }

    @Override
    public UUID id() {
        String name = authentication().getName();
        try {
            return UUID.fromString(name);                       // the token subject is the user id
        } catch (IllegalArgumentException notAUuid) {
            return users.findByEmail(EmailAddress.of(name))     // the token subject is the email
                    .map(u -> u.id().value())
                    .orElseThrow(SecurityCurrentUser::unauthenticated);
        }
    }

    @Override
    public boolean hasAuthority(String authority) {
        return authentication().getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private static Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw unauthenticated();
        }
        return auth;
    }

    private static DomainException unauthenticated() {
        return new DomainException(IdentityError.INVALID_CREDENTIALS, "Not authenticated");
    }
}