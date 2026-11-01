package com.procureflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private final Instant now = Instant.parse("2026-10-28T10:00:00Z");

    @Test
    void rotationRevokesOldAndLinksToNew() {
        RefreshToken first = RefreshToken.issue(UserId.newId(), null, "h1", now.plusSeconds(600));
        RefreshToken second = RefreshToken.issue(first.userId(), first.familyId(), "h2", now.plusSeconds(600));

        first.rotateTo(second);

        assertThat(first.isRevoked()).isTrue();
        assertThat(first.replacedBy()).isEqualTo(second.id());
        assertThat(second.familyId()).isEqualTo(first.familyId());
    }

    @Test
    void expiryIsChecked() {
        RefreshToken t = RefreshToken.issue(UserId.newId(), null, "h", now.plusSeconds(60));
        assertThat(t.isExpired(now)).isFalse();
        assertThat(t.isExpired(now.plusSeconds(60))).isTrue();
    }
}