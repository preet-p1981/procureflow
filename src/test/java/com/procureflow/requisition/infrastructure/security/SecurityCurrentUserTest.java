package com.procureflow.requisition.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityCurrentUserTest {

    private UserRepository users;
    private SecurityCurrentUser currentUser;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        currentUser = new SecurityCurrentUser(users);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String name, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(name, null, granted));
    }

    @Test
    void usesSubjectWhenItIsAUserId() {
        UUID id = UUID.randomUUID();
        loginAs(id.toString());
        assertThat(currentUser.id()).isEqualTo(id);
    }

    @Test
    void looksUpUserByEmailWhenSubjectIsAnEmail() {
        User user = User.register(EmailAddress.of("preet@example.com"), "hash", "Preet", Set.<Role>of());
        when(users.findByEmail(EmailAddress.of("preet@example.com"))).thenReturn(Optional.of(user));
        loginAs("Preet@Example.com");
        assertThat(currentUser.id()).isEqualTo(user.id().value());
    }

    @Test
    void unknownEmailIsNotAuthenticated() {
        when(users.findByEmail(EmailAddress.of("nobody@example.com"))).thenReturn(Optional.empty());
        loginAs("nobody@example.com");
        assertThatThrownBy(currentUser::id).isInstanceOf(DomainException.class);
    }

    @Test
    void noLoginMeansNotAuthenticated() {
        assertThatThrownBy(currentUser::id).isInstanceOf(DomainException.class);
    }

    @Test
    void checksAuthoritiesOfTheCurrentLogin() {
        loginAs(UUID.randomUUID().toString(), "REQ_CREATE", "ROLE_REQUESTER");
        assertThat(currentUser.hasAuthority("REQ_CREATE")).isTrue();
        assertThat(currentUser.hasAuthority("REQ_READ_ALL")).isFalse();
    }
}