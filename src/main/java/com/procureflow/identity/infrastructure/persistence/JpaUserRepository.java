package com.procureflow.identity.infrastructure.persistence;

import com.procureflow.identity.domain.*;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional
class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository users;
    private final SpringDataRoleRepository roles;

    JpaUserRepository(SpringDataUserRepository users,SpringDataRoleRepository roles){
        this.users = users;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id){
        return users.findById(id.value()).map(IdentityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(EmailAddress email) {
        return users.findByEmailIgnoreCase(email.value()).map(IdentityMapper::toDomain);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(EmailAddress email){
        return users.existsByEmailIgnoreCase(email.value());
    }

    @Override
    public User save(User user){
        UserEntity entity = users.findById(user.id().value())
                .orElseGet(()-> new UserEntity(user.id().value()));

        boolean isExisting = entity.getVersion() != null;
        if(isExisting && entity.getVersion() != user.version()){
            throw new DomainException(CommonError.CONFLICT,"User was modified by someone reload and try again");
        }

        entity.setEmail(user.email().value());
        entity.setPasswordHash(user.passwordHash());
        entity.setFullName(user.fullName());
        entity.setStatus(user.status());
        entity.setFailedAttempts(user.failedAttempts());
        entity.setLockedUntil(user.lockedUntil());
        entity.setRoles(resolveRoles(user));

        try{
            return IdentityMapper.toDomain((users.saveAndFlush(entity)));

        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(IdentityError.EMAIL_ALREADY_REGISTERED,"Email is already registered");
        }

    }
    private Set<RoleEntity> resolveRoles(User user){
        Set<String> codes = user.roles().stream().map(Role::code).collect(Collectors.toSet());
        List<RoleEntity> found = roles.findAllById(codes);
        if(found.size() != codes.size()){
            throw new DomainException(CommonError.VALIDATION_FAILED,"Unknown role in " + codes);
        }
        return new HashSet<>(found);
    }
}
