package com.procureflow.approval.infrastructure.persistence;

import com.procureflow.approval.application.InboxItem;
import com.procureflow.approval.application.port.ApprovalInboxQuery;
import com.procureflow.approval.domain.ApprovalStatus;
import com.procureflow.approval.domain.StepDecision;
import com.procureflow.shared.application.PageResult;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class JpaApprovalInboxQuery implements ApprovalInboxQuery {

    private final SpringDataApprovalRepository repo;

    JpaApprovalInboxQuery(SpringDataApprovalRepository repo) {
        this.repo = repo;
    }

    @Override
    public PageResult<InboxItem> pending(Set<String> roles, UUID currentUserId, int page, int size) {
        if (roles == null || roles.isEmpty()) {
            return new PageResult<>(List.of(), page, size, 0, 0);     // "in ()" with no values is invalid SQL
        }
        Page<InboxItem> result = repo.findInbox(ApprovalStatus.PENDING, StepDecision.PENDING,
                roles, currentUserId, PageRequest.of(page, size));
        return new PageResult<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}