package com.procureflow.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.RefreshToken;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.RefreshTokenRepository;
import com.procureflow.identity.domain.port.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaRefreshTokenRepositoryTest {

    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;

    private User newUser() {
        return users.save(User.register(
                EmailAddress.of("rt-" + UUID.randomUUID() + "@example.com"), "HASH", "Preet", Set.of()));
    }

    @Test
    void savedTokenCanBeFoundByHash() {
        User u = newUser();
        String hash = UUID.randomUUID().toString().replace("-", "");
        tokens.save(RefreshToken.issue(u.id(), null, hash, Instant.now().plusSeconds(600)));

        var found = tokens.findByTokenHash(hash);

        assertThat(found).isPresent();
        assertThat(found.get().isRevoked()).isFalse();
        assertThat(found.get().userId()).isEqualTo(u.id());
    }

    @Test
    void revokeFamilyRevokesEveryTokenInIt() {
        User u = newUser();
        String h1 = UUID.randomUUID().toString().replace("-", "");
        String h2 = UUID.randomUUID().toString().replace("-", "");
        RefreshToken first = tokens.save(RefreshToken.issue(u.id(), null, h1, Instant.now().plusSeconds(600)));
        tokens.save(RefreshToken.issue(u.id(), first.familyId(), h2, Instant.now().plusSeconds(600)));

        tokens.revokeFamily(first.familyId());

        assertThat(tokens.findByTokenHash(h1).orElseThrow().isRevoked()).isTrue();
        assertThat(tokens.findByTokenHash(h2).orElseThrow().isRevoked()).isTrue();
    }
}