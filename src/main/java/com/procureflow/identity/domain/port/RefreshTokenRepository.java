package com.procureflow.identity.domain.port;

import com.procureflow.identity.domain.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeFamily(UUID familyId);
}