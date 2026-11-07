package com.procureflow.approval.application;

import com.procureflow.approval.domain.SubjectType;
import java.time.Instant;
import java.util.UUID;

public record InboxItem(UUID instanceId, SubjectType subjectType, UUID subjectId, UUID requesterId,
                        int stepNo, String approverRole, Instant slaDueAt, Instant createdAt) {

    public boolean overdue(Instant now) {
        return now.isAfter(slaDueAt);
    }
}