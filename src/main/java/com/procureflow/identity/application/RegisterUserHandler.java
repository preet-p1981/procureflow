package com.procureflow.identity.application;

import com.procureflow.identity.application.command.RegisterUserCommand;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.PasswordPolicy;
import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.PasswordHasher;
import com.procureflow.identity.domain.port.RoleRepository;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserHandler {

    static final String DEFAULT_ROLE = "REQUESTER";

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher hasher;

    public RegisterUserHandler(UserRepository users, RoleRepository roles, PasswordHasher hasher) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
    }

    @Transactional
    public RegisteredUser handle(RegisterUserCommand cmd) {
        PasswordPolicy.validate(cmd.rawPassword());
        EmailAddress email = EmailAddress.of(cmd.email());

        if (users.existsByEmail(email)) {
            throw new DomainException(IdentityError.EMAIL_ALREADY_REGISTERED, "Email is already registered");
        }

        Role role = roles.findByCode(DEFAULT_ROLE).orElseThrow(() ->
                new DomainException(CommonError.VALIDATION_FAILED, "Default role missing: " + DEFAULT_ROLE));

        User saved = users.save(
                User.register(email, hasher.hash(cmd.rawPassword()), cmd.fullName(), Set.of(role)));
        return new RegisteredUser(saved.id(), saved.email(), saved.status());
    }
}