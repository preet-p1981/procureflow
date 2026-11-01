package com.procureflow.identity.application;

import com.procureflow.identity.application.command.ExternalLoginCommand;
import com.procureflow.identity.application.port.AccessTokenIssuer;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.ExternalIdentityRepository;
import com.procureflow.identity.domain.port.PasswordHasher;
import com.procureflow.identity.domain.port.RoleRepository;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalLoginHandler {

    private final ExternalIdentityRepository identities;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenService refreshTokens;
    private final Clock clock;

    public ExternalLoginHandler(ExternalIdentityRepository identities, UserRepository users,
                                RoleRepository roles, PasswordHasher hasher,
                                AccessTokenIssuer accessTokens, RefreshTokenService refreshTokens,
                                Clock clock) {
        this.identities = identities;
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Transactional
    public LoginResult handle(ExternalLoginCommand cmd) {
        Instant now = clock.instant();

        User user = identities.findUserId(cmd.provider(), cmd.subject())
                .flatMap(users::findById)
                .orElseGet(() -> linkOrCreate(cmd));

        user.checkCanAuthenticate(now);
        user.recordSuccessfulLogin();
        User saved = users.save(user);

        var access = accessTokens.issue(saved.id().value().toString(),
                saved.email().value(), saved.authorities());
        var refresh = refreshTokens.issue(saved.id(), null, now);
        return new LoginResult(access, refresh.raw());
    }

    private User linkOrCreate(ExternalLoginCommand cmd) {
        if (!cmd.emailVerified()) {
            throw new DomainException(IdentityError.INVALID_CREDENTIALS, "Email is not verified by the provider");
        }
        EmailAddress email = EmailAddress.of(cmd.email());

        User user = users.findByEmail(email).orElseGet(() -> createUser(email, cmd.fullName()));
        identities.link(user.id(), cmd.provider(), cmd.subject());
        return user;
    }

    private User createUser(EmailAddress email, String fullName) {
        Role role = roles.findByCode(RegisterUserHandler.DEFAULT_ROLE).orElseThrow(() ->
                new DomainException(CommonError.VALIDATION_FAILED, "Default role missing"));
        String name = (fullName == null || fullName.isBlank()) ? email.value() : fullName;
        String unusableHash = hasher.hash(UUID.randomUUID().toString());
        return users.save(User.register(email, unusableHash, name, Set.of(role)));
    }
}