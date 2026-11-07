package com.procureflow.approval.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Role codes are without the ROLE_ prefix, for example DEPARTMENT_MANAGER. */
public record ApproverContext(UUID userId, Set<String> roles) {

    public ApproverContext {
        Objects.requireNonNull(userId);
        roles = Set.copyOf(roles == null ? Set.of() : roles);
    }
}