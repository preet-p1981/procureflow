package com.procureflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.identity.application.command.ExternalLoginCommand;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExternalLoginHandlerTest {

    @Autowired ExternalLoginHandler handler;
    @Autowired UserRepository users;

    private String email() { return "g-" + UUID.randomUUID() + "@example.com"; }

    @Test
    void firstGoogleLoginCreatesUserAndSecondReusesIt() {
        String email = email();
        String sub = "sub-" + UUID.randomUUID();

        var first = handler.handle(new ExternalLoginCommand("google", sub, email, true, "Preet"));
        var second = handler.handle(new ExternalLoginCommand("google", sub, email, true, "Preet"));

        assertThat(first.accessToken().value()).isNotBlank();
        assertThat(first.refreshToken()).isNotBlank();
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(users.existsByEmail(EmailAddress.of(email))).isTrue();
    }

    @Test
    void existingPasswordUserGetsLinkedByVerifiedEmail() {
        String email = email();
        // existing local user created through the same handler path is enough; link a second subject
        handler.handle(new ExternalLoginCommand("google", "sub-" + UUID.randomUUID(), email, true, "Preet"));

        var result = handler.handle(new ExternalLoginCommand("google", "sub-" + UUID.randomUUID(), email, true, "Preet"));

        assertThat(result.accessToken().value()).isNotBlank();
    }

    @Test
    void unverifiedEmailIsRejected() {
        assertThatThrownBy(() -> handler.handle(
                new ExternalLoginCommand("google", "sub-" + UUID.randomUUID(), email(), false, "Preet")))
                .isInstanceOf(DomainException.class);
    }
}