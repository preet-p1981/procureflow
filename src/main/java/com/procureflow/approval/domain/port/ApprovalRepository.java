package com.procureflow.approval.domain.port;

import com.procureflow.approval.domain.ApprovalInstance;
import com.procureflow.approval.domain.SubjectType;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository {
    Optional<ApprovalInstance> findById(UUID id);

    /** The chain currently in progress for this subject, if any. */
    Optional<ApprovalInstance> findActiveBySubject(SubjectType subjectType, UUID subjectId);

    ApprovalInstance save(ApprovalInstance instance);
}