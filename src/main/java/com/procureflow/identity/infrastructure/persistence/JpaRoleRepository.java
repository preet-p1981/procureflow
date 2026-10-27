package com.procureflow.identity.infrastructure.persistence;

import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.port.RoleRepository;
import lombok.Locked;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
 class JpaRoleRepository implements RoleRepository {

    private final SpringDataRoleRepository jpa;

    JpaRoleRepository(SpringDataRoleRepository jpa){
        this.jpa = jpa;
    }

    @Override
    public Optional<Role> findByCode(String code){
        return jpa.findById(code).map(IdentityMapper::toDomain);
    }
}
