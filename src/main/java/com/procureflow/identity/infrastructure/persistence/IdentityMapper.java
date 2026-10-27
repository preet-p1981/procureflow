package com.procureflow.identity.infrastructure.persistence;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.UserId;

import java.util.Set;
import java.util.stream.Collectors;

final class IdentityMapper {

    private IdentityMapper(){};

    static Role toDomain(RoleEntity entity){
        return new Role(entity.getCode(), Set.copyOf(entity.getPermissionCodes()));
    }

    static User toDomain(UserEntity entity){
        Set<Role> roles = entity.getRoles().stream()
                .map(IdentityMapper::toDomain)
                .collect(Collectors.toSet());
        return User.reconstitute(
                new UserId((entity.getId())),
                EmailAddress.of(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getStatus(),
                entity.getFailedAttempts(),
                entity.getLockedUntil(),
                roles,
                entity.getVersion() == null ? 0L : entity.getVersion());
    }
}
