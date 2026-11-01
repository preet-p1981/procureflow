package com.procureflow.identity.infrastructure.persistence;

import com.procureflow.identity.domain.UserId;
import com.procureflow.identity.domain.port.ExternalIdentityRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaExternalIdentityRepository implements ExternalIdentityRepository {

    private final SpringDataUserIdentityRepository jpa;

    JpaExternalIdentityRepository(SpringDataUserIdentityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserId> findUserId(String provider, String subject) {
        return jpa.findByProviderAndProviderSubject(provider, subject)
                .map(e -> new UserId(e.getUserId()));
    }

    @Override
    @Transactional
    public void link(UserId userId, String provider, String subject) {
        jpa.saveAndFlush(new UserIdentityEntity(UUID.randomUUID(), userId.value(), provider, subject));
    }
}