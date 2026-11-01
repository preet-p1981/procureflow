package com.procureflow.identity.domain.port;

import com.procureflow.identity.domain.UserId;

import java.util.Optional;

public interface ExternalIdentityRepository {
    Optional<UserId> findUserId(String provider , String subject);
    void link(UserId userId,String provider , String subject);
}
