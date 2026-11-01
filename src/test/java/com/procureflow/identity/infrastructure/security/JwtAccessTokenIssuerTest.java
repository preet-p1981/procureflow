package com.procureflow.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.procureflow.identity.application.port.AccessToken;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAccessTokenIssuerTest {

    private final JwtProperties props = new JwtProperties(
            "p7qTmd5CnccnmT9H0YQFr6lMAROoItIe28Yn7dKWZ0=", 15L, "https://procureflow.dev");
    private final JwtConfig config = new JwtConfig();

    @Test
    void issuedTokenDecodesToSameClaims() {
        var issuer = new JwtAccessTokenIssuer(config.jwtEncoder(props), props);

        AccessToken token = issuer.issue("42", "a@b.com", List.of("USER_READ", "ROLE_ADMIN"));
        Jwt jwt = config.jwtDecoder(props).decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("a@b.com");
        assertThat(jwt.getClaimAsStringList("authorities")).containsExactly("USER_READ", "ROLE_ADMIN");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://procureflow.dev");
        assertThat(token.expiresAt()).isAfter(Instant.now().plusSeconds(14 * 60));
    }
}