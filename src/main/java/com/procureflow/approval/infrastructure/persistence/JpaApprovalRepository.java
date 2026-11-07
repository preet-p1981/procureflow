package com.procureflow.approval.infrastructure.persistence;

import com.procureflow.approval.domain.ApprovalError;
import com.procureflow.approval.domain.ApprovalInstance;
import com.procureflow.approval.domain.ApprovalStatus;
import com.procureflow.approval.domain.ApprovalStep;
import com.procureflow.approval.domain.SubjectType;
import com.procureflow.approval.domain.port.ApprovalRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
class JpaApprovalRepository implements ApprovalRepository {

    private final SpringDataApprovalRepository repo;

    JpaApprovalRepository(SpringDataApprovalRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApprovalInstance> findById(UUID id) {
        return repo.findById(id).map(JpaApprovalRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApprovalInstance> findActiveBySubject(SubjectType subjectType, UUID subjectId) {
        return repo.findBySubjectTypeAndSubjectIdAndStatus(subjectType, subjectId, ApprovalStatus.PENDING)
                .map(JpaApprovalRepository::toDomain);
    }

    @Override
    public ApprovalInstance save(ApprovalInstance a) {
        ApprovalInstanceEntity entity = repo.findById(a.id()).orElse(null);

        if (entity == null) {
            return insert(a);
        }
        if (entity.getVersion() != a.version()) {
            throw new DomainException(CommonError.CONFLICT,
                    "This approval was changed by someone else, reload and try again");
        }

        entity.applyState(a.status(), a.currentStepNo(), a.completedAt());
        for (ApprovalStep step : a.steps()) {
            entity.getSteps().stream()
                    .filter(s -> s.getStepNo() == step.stepNo())
                    .findFirst()
                    .ifPresent(s -> s.applyDecision(step.decision(), step.decidedBy(), step.decidedAt(), step.comment()));
        }
        repo.flush();
        return toDomain(entity);
    }

    private ApprovalInstance insert(ApprovalInstance a) {
        ApprovalInstanceEntity entity = new ApprovalInstanceEntity(a.id(), a.subjectType(), a.subjectId(),
                a.requesterId(), a.createdAt());
        entity.applyState(a.status(), a.currentStepNo(), a.completedAt());
        for (ApprovalStep step : a.steps()) {
            ApprovalStepEntity stepEntity = new ApprovalStepEntity(step.stepNo(), step.approverRole(), step.slaDueAt());
            stepEntity.applyDecision(step.decision(), step.decidedBy(), step.decidedAt(), step.comment());
            entity.addStep(stepEntity);
        }
        try {
            repo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            // the partial unique index allows only one PENDING chain per subject
            throw new DomainException(ApprovalError.INVALID_APPROVAL_STATE,
                    "An approval is already in progress for this request");
        }
        return toDomain(entity);
    }

    private static ApprovalInstance toDomain(ApprovalInstanceEntity e) {
        List<ApprovalStep> steps = e.getSteps().stream()
                .map(s -> ApprovalStep.reconstitute(s.getStepNo(), s.getApproverRole(), s.getSlaDueAt(),
                        s.getDecision(), s.getDecidedBy(), s.getDecidedAt(), s.getComment()))
                .toList();
        return ApprovalInstance.reconstitute(e.getId(), e.getSubjectType(), e.getSubjectId(), e.getRequesterId(),
                e.getStatus(), e.getCurrentStep(), steps, e.getCreatedAt(), e.getCompletedAt(),
                e.getVersion() == null ? 0L : e.getVersion());
    }
}