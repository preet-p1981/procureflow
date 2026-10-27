package com.procureflow.identity.domain.port;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.UserId;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(EmailAddress email);
    boolean existsByEmail(EmailAddress email);
    User save(User user);
}
