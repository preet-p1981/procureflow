package com.procureflow.approval.infrastructure.persistence;

import com.procureflow.approval.application.InboxItem;
import com.procureflow.approval.domain.ApprovalStatus;
import com.procureflow.approval.domain.StepDecision;
import com.procureflow.approval.domain.SubjectType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataApprovalRepository extends JpaRepository<ApprovalInstanceEntity, UUID> {

    Optional<ApprovalInstanceEntity> findBySubjectTypeAndSubjectIdAndStatus(
            SubjectType subjectType, UUID subjectId, ApprovalStatus status);

    @Query(value = """
            select new com.procureflow.approval.application.InboxItem(
                   i.id, i.subjectType, i.subjectId, i.requesterId, s.stepNo, s.approverRole, s.slaDueAt, i.createdAt)
              from ApprovalInstanceEntity i join i.steps s
             where i.status = :pending and s.stepNo = i.currentStep and s.decision = :stepPending
               and s.approverRole in :roles and i.requesterId <> :me
             order by s.slaDueAt asc
            """,
            countQuery = """
            select count(i)
              from ApprovalInstanceEntity i join i.steps s
             where i.status = :pending and s.stepNo = i.currentStep and s.decision = :stepPending
               and s.approverRole in :roles and i.requesterId <> :me
            """)
    Page<InboxItem> findInbox(@Param("pending") ApprovalStatus pending,
                              @Param("stepPending") StepDecision stepPending,
                              @Param("roles") Collection<String> roles,
                              @Param("me") UUID me,
                              Pageable pageable);
}