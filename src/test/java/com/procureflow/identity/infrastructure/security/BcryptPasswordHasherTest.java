package com.procureflow.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptPasswordHasherTest {

    // low strength (4) only to keep the unit test fast
    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(new BCryptPasswordEncoder(4));

    @Test
    void hashIsNotThePlainPassword() {
        assertThat(hasher.hash("Secret#123")).isNotEqualTo("Secret#123").startsWith("$2");
    }

    @Test
    void sameInputGivesDifferentHashesBecauseOfSalt() {
        assertThat(hasher.hash("Secret#123")).isNotEqualTo(hasher.hash("Secret#123"));
    }

    @Test
    void matchesCorrectPasswordOnly() {
        String hash = hasher.hash("Secret#123");
        assertThat(hasher.matches("Secret#123", hash)).isTrue();
        assertThat(hasher.matches("wrong", hash)).isFalse();
    }
}