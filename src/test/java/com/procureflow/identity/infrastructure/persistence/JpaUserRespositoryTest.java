package com.procureflow.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.RoleRepository;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional   // every test rolls back, so the test database stays clean
class JpaUserRepositoryTest {

    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

    private User newUser(String email, String roleCode) {
        Role role = roles.findByCode(roleCode).orElseThrow();
        return User.register(EmailAddress.of(email), "hash-value", "Test User", Set.of(role));
    }

    @Test
    void seededRoleLoadsWithItsPermissions() {
        Role admin = roles.findByCode("ADMIN").orElseThrow();
        assertThat(admin.permission()).contains("USER_READ", "USER_MANAGE", "ROLE_MANAGE");
    }

    @Test
    void savesAndLoadsUserWithRoles() {
        User saved = users.save(newUser("Preet@Example.com", "ADMIN"));

        User loaded = users.findByEmail(EmailAddress.of("preet@example.com")).orElseThrow();
        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.email().value()).isEqualTo("preet@example.com");
        assertThat(loaded.authorities()).contains("ROLE_ADMIN", "USER_READ");
    }

    @Test
    void persistsLockoutState() {
        User user = users.save(newUser("lock@example.com", "REQUESTER"));
        user.recordFailedLogin(java.time.Instant.now(),
                new com.procureflow.identity.domain.LockoutPolicy(1, java.time.Duration.ofMinutes(15)));
        users.save(user);

        User loaded = users.findById(user.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(com.procureflow.identity.domain.UserStatus.LOCKED);
        assertThat(loaded.lockedUntil()).isNotNull();
    }

    @Test
    void updateIncreasesVersion() {
        User first = users.save(newUser("ver@example.com", "REQUESTER"));
        first.disable();
        User second = users.save(first);
        assertThat(second.version()).isGreaterThan(first.version());
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        users.save(newUser("dup@example.com", "REQUESTER"));
        assertThatThrownBy(() -> users.save(newUser("DUP@example.com", "REQUESTER")))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(IdentityError.EMAIL_ALREADY_REGISTERED);
    }

    @Test
    void rejectsStaleVersion() {
        User saved = users.save(newUser("stale@example.com", "REQUESTER"));
        User copyA = users.findById(saved.id()).orElseThrow();
        User copyB = users.findById(saved.id()).orElseThrow();

        copyA.disable();
        users.save(copyA);                       // moves the version forward

        copyB.assignRole(Role.of("AUDITOR", "AUDIT_READ"));
        assertThatThrownBy(() -> users.save(copyB))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CommonError.CONFLICT);
    }
}