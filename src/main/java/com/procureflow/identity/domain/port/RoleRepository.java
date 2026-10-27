package com.procureflow.identity.domain.port;

import com.procureflow.identity.domain.Role;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByCode(String code);
}