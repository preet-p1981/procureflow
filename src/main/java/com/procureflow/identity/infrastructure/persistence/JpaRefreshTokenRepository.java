package com.procureflow.identity.infrastructure.persistence;

import com.procureflow.identity.domain.RefreshToken;
import com.procureflow.identity.domain.UserId;
import com.procureflow.identity.domain.port.RefreshTokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpa;

    JpaRefreshTokenRepository(SpringDataRefreshTokenRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken t) {
        jpa.save(new RefreshTokenEntity(t.id(), t.userId().value(), t.familyId(), t.tokenHash(),
                t.expiresAt(), t.isRevoked(), t.replacedBy()));
        return t;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(e -> RefreshToken.reconstitute(
                e.getId(), new UserId(e.getUserId()), e.getFamilyId(), e.getTokenHash(),
                e.getExpiresAt(), e.isRevoked(), e.getReplacedBy()));
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId) {
        jpa.revokeFamily(familyId);
    }
}