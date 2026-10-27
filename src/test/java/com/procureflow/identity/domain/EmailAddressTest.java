package com.procureflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

    @Test
    void trimsAndLowercases() {
        assertThat(EmailAddress.of("  Preet@Example.COM ").value())
                .isEqualTo("preet@example.com");
    }

    @Test
    void sameEmailInDifferentCaseIsEqual() {
        assertThat(EmailAddress.of("A@b.com")).isEqualTo(EmailAddress.of("a@B.COM"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "no-at-sign", "a@b", "a b@c.com", "@c.com", "a@@c.com"})
    void rejectsInvalidEmails(String input) {
        assertThatThrownBy(() -> EmailAddress.of(input))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(IdentityError.INVALID_EMAIL);
    }

    @Test
    void rejectsTooLongEmail() {
        String longEmail = "a".repeat(250) + "@b.com";
        assertThatThrownBy(() -> EmailAddress.of(longEmail))
                .isInstanceOf(DomainException.class);
    }
}