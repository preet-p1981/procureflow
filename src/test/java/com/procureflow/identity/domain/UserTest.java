package com.procureflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");
    private static final LockoutPolicy POLICY = new LockoutPolicy(3, Duration.ofMinutes(15));

    private User newUser() {
        return User.register(EmailAddress.of("user@example.com"), "hash-value",
                "Test User", Set.of(Role.of("REQUESTER")));
    }

    private void failTimes(User user, int times, Instant at) {
        for (int i = 0; i < times; i++) {
            user.recordFailedLogin(at, POLICY);
        }
    }

    @Test
    void newUserIsActiveWithNoFailures() {
        User user = newUser();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.failedAttempts()).isZero();
        assertThat(user.isLocked(NOW)).isFalse();
    }

    @Test
    void doesNotLockBeforeLimit() {
        User user = newUser();
        failTimes(user, 2, NOW);
        assertThat(user.isLocked(NOW)).isFalse();
        assertThat(user.failedAttempts()).isEqualTo(2);
    }

    @Test
    void locksWhenLimitReached() {
        User user = newUser();
        failTimes(user, 3, NOW);
        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThatThrownBy(() -> user.checkCanAuthenticate(NOW))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(IdentityError.ACCOUNT_LOCKED);
    }

    @Test
    void lockExpiresAfterDuration() {
        User user = newUser();
        failTimes(user, 3, NOW);
        Instant later = NOW.plus(Duration.ofMinutes(16));
        assertThat(user.isLocked(later)).isFalse();
        user.checkCanAuthenticate(later);   // must not throw
    }

    @Test
    void failureAfterExpiredLockStartsCountingAgain() {
        User user = newUser();
        failTimes(user, 3, NOW);
        user.recordFailedLogin(NOW.plus(Duration.ofMinutes(16)), POLICY);
        assertThat(user.failedAttempts()).isEqualTo(1);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void successfulLoginResetsCounter() {
        User user = newUser();
        failTimes(user, 2, NOW);
        user.recordSuccessfulLogin();
        assertThat(user.failedAttempts()).isZero();
    }

    @Test
    void disabledUserCannotAuthenticate() {
        User user = newUser();
        user.disable();
        assertThatThrownBy(() -> user.checkCanAuthenticate(NOW))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(IdentityError.ACCOUNT_DISABLED);
    }

    @Test
    void authoritiesContainRolesAndPermissions() {
        User user = newUser();
        user.assignRole(Role.of("ADMIN", "USER_READ", "USER_MANAGE"));
        assertThat(user.authorities())
                .containsExactlyInAnyOrder("ROLE_REQUESTER", "ROLE_ADMIN", "USER_READ", "USER_MANAGE");
    }

    @Test
    void removingRoleRemovesItsAuthorities() {
        User user = newUser();
        user.assignRole(Role.of("ADMIN", "USER_READ"));
        user.removeRole("ADMIN");
        assertThat(user.authorities()).containsExactly("ROLE_REQUESTER");
    }

    @Test
    void rolesViewCannotBeModified() {
        User user = newUser();
        assertThatThrownBy(() -> user.roles().add(Role.of("ADMIN")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankFullName() {
        assertThatThrownBy(() -> User.register(EmailAddress.of("a@b.com"), "hash", "  ", Set.of()))
                .isInstanceOf(DomainException.class);
    }
}