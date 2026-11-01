package com.procureflow.identity.application;

import com.procureflow.identity.application.command.LoginCommand;
import com.procureflow.identity.application.port.AccessTokenIssuer;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.LockoutPolicy;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.PasswordHasher;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHandler {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer tokens;
    private final RefreshTokenService refreshTokens;
    private final LockoutPolicy lockoutPolicy;
    private final Clock clock;

    public LoginHandler(UserRepository users, PasswordHasher hasher, AccessTokenIssuer tokens,
                        RefreshTokenService refreshTokens, LockoutPolicy lockoutPolicy, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.lockoutPolicy = lockoutPolicy;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public LoginResult handle(LoginCommand cmd) {
        Instant now = clock.instant();

        User user = users.findByEmail(EmailAddress.of(cmd.email()))
                .orElseThrow(LoginHandler::invalidCredentials);

        user.checkCanAuthenticate(now);

        if (!hasher.matches(cmd.rawPassword(), user.passwordHash())) {
            user.recordFailedLogin(now, lockoutPolicy);
            users.save(user);
            throw invalidCredentials();
        }

        user.recordSuccessfulLogin();
        User saved = users.save(user);

        var access = tokens.issue(saved.id().value().toString(), saved.email().value(), saved.authorities());
        var refresh = refreshTokens.issue(saved.id(), null, now);
        return new LoginResult(access, refresh.raw());
    }

    private static DomainException invalidCredentials() {
        return new DomainException(IdentityError.INVALID_CREDENTIALS, "Invalid email or password");
    }
}