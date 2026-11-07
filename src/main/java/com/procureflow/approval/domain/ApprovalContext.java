package com.procureflow.approval.domain;

import com.procureflow.shared.domain.Money;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Role codes are without the ROLE_ prefix. */
public record ApprovalContext(SubjectType subjectType, UUID subjectId, UUID requesterId, UUID departmentId,
                              Money total, Set<String> requesterRoles) {

    public ApprovalContext {
        Objects.requireNonNull(subjectType);
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(requesterId);
        Objects.requireNonNull(departmentId);
        Objects.requireNonNull(total);
        requesterRoles = Set.copyOf(requesterRoles == null ? Set.of() : requesterRoles);
    }
}