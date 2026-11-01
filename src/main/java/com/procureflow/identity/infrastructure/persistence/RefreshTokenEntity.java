package com.procureflow.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    protected RefreshTokenEntity() {}

    RefreshTokenEntity(UUID id, UUID userId, UUID familyId, String tokenHash,
                       Instant expiresAt, boolean revoked, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.replacedBy = replacedBy;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    UUID getFamilyId() { return familyId; }
    String getTokenHash() { return tokenHash; }
    Instant getExpiresAt() { return expiresAt; }
    boolean isRevoked() { return revoked; }
    UUID getReplacedBy() { return replacedBy; }
}