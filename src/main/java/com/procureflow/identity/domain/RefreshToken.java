package com.procureflow.identity.domain;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    private final UUID id;
    private final UserId userId;
    private final UUID familyId;
    private final String tokenHash;
    private final Instant expiresAt;
    private boolean revoked;
    private UUID replacedBy;

    private RefreshToken(UUID id, UserId userId, UUID familyId, String tokenHash,
                         Instant expiresAt, boolean revoked, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.replacedBy = replacedBy;
    }

    /** A new token. First login passes familyId = null (a new family starts). */
    public static RefreshToken issue(UserId userId, UUID familyId, String tokenHash, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        return new RefreshToken(id, userId, familyId == null ? UUID.randomUUID() : familyId,
                tokenHash, expiresAt, false, null);
    }

    public static RefreshToken reconstitute(UUID id, UserId userId, UUID familyId, String tokenHash,
                                            Instant expiresAt, boolean revoked, UUID replacedBy) {
        return new RefreshToken(id, userId, familyId, tokenHash, expiresAt, revoked, replacedBy);
    }

    public boolean isExpired(Instant now) { return !now.isBefore(expiresAt); }

    /** Marks this token as used up, replaced by the next one in the chain. */
    public void rotateTo(RefreshToken next) {
        this.revoked = true;
        this.replacedBy = next.id;
    }

    public void revoke() { this.revoked = true; }

    public UUID id() { return id; }
    public UserId userId() { return userId; }
    public UUID familyId() { return familyId; }
    public String tokenHash() { return tokenHash; }
    public Instant expiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public UUID replacedBy() { return replacedBy; }
}