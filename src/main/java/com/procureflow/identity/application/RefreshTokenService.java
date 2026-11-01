package com.procureflow.identity.application;

import com.procureflow.identity.domain.RefreshToken;
import com.procureflow.identity.domain.RefreshTokenPolicy;
import com.procureflow.identity.domain.UserId;
import com.procureflow.identity.domain.port.RefreshTokenGenerator;
import com.procureflow.identity.domain.port.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final RefreshTokenGenerator generator;
    private final RefreshTokenPolicy policy;

    RefreshTokenService(RefreshTokenRepository repo, RefreshTokenGenerator generator, RefreshTokenPolicy policy) {
        this.repo = repo;
        this.generator = generator;
        this.policy = policy;
    }

    /** Creates and stores a new token. familyId null = new family. Returns the stored token and the raw value. */
    Issued issue(UserId userId, UUID familyId, Instant now) {
        String raw = generator.generateRaw();
        RefreshToken token = repo.save(
                RefreshToken.issue(userId, familyId, generator.hash(raw), now.plus(policy.ttl())));
        return new Issued(token, raw);
    }

    record Issued(RefreshToken token, String raw) {}
}