package com.procureflow.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.ExternalIdentityRepository;
import com.procureflow.identity.domain.port.UserRepository;
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
class JpaExternalIdentityRepositoryTest {

    @Autowired UserRepository users;
    @Autowired ExternalIdentityRepository identities;

    private User newUser() {
        return users.save(User.register(
                EmailAddress.of("ext-" + UUID.randomUUID() + "@example.com"), "HASH", "Preet", Set.of()));
    }

    @Test
    void linkedIdentityIsFoundByProviderAndSubject() {
        User u = newUser();
        String subject = "sub-" + UUID.randomUUID();

        identities.link(u.id(), "google", subject);

        assertThat(identities.findUserId("google", subject)).contains(u.id());
    }

    @Test
    void unknownSubjectIsEmpty() {
        assertThat(identities.findUserId("google", "nope-" + UUID.randomUUID())).isEmpty();
    }

    @Test
    void sameSubjectCannotBeLinkedTwice() {
        User a = newUser();
        User b = newUser();
        String subject = "sub-" + UUID.randomUUID();
        identities.link(a.id(), "google", subject);

        assertThatThrownBy(() -> identities.link(b.id(), "google", subject))
                .isInstanceOf(Exception.class);
    }
}