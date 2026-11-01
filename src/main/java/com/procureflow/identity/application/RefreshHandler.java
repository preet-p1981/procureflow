package com.procureflow.identity.application;

import com.procureflow.identity.application.port.AccessTokenIssuer;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.RefreshToken;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.RefreshTokenGenerator;
import com.procureflow.identity.domain.port.RefreshTokenRepository;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshHandler {

    private final RefreshTokenRepository repo;
    private final RefreshTokenGenerator generator;
    private final RefreshTokenService refreshTokens;
    private final UserRepository users;
    private final AccessTokenIssuer accessTokens;
    private final Clock clock;

    public RefreshHandler(RefreshTokenRepository repo, RefreshTokenGenerator generator,
                          RefreshTokenService refreshTokens, UserRepository users,
                          AccessTokenIssuer accessTokens, Clock clock) {
        this.repo = repo;
        this.generator = generator;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.accessTokens = accessTokens;
        this.clock = clock;
    }

    // noRollbackFor: the family revoke must be committed even though we throw
    @Transactional(noRollbackFor = DomainException.class)
    public LoginResult handle(String rawToken) {
        Instant now = clock.instant();

        RefreshToken old = repo.findByTokenHash(generator.hash(rawToken))
                .orElseThrow(() -> new DomainException(IdentityError.INVALID_REFRESH_TOKEN, "Invalid refresh token"));

        if (old.isRevoked()) {
            repo.revokeFamily(old.familyId());   // reuse detected: kill the whole family
            throw new DomainException(IdentityError.REFRESH_TOKEN_REUSED, "Refresh token was already used");
        }
        if (old.isExpired(now)) {
            throw new DomainException(IdentityError.INVALID_REFRESH_TOKEN, "Refresh token expired");
        }

        User user = users.findById(old.userId())
                .orElseThrow(() -> new DomainException(IdentityError.INVALID_REFRESH_TOKEN, "Invalid refresh token"));
        user.checkCanAuthenticate(now);

        var next = refreshTokens.issue(user.id(), old.familyId(), now);
        old.rotateTo(next.token());
        repo.save(old);

        var access = accessTokens.issue(user.id().value().toString(), user.email().value(), user.authorities());
        return new LoginResult(access, next.raw());
    }
}