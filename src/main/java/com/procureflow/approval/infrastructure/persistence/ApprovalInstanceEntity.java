package com.procureflow.approval.infrastructure.persistence;

import com.procureflow.approval.domain.ApprovalStatus;
import com.procureflow.approval.domain.SubjectType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "approval_instance")
public class ApprovalInstanceEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 30, updatable = false)
    private SubjectType subjectType;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo ASC")
    private List<ApprovalStepEntity> steps = new ArrayList<>();

    protected ApprovalInstanceEntity() {
    }

    ApprovalInstanceEntity(UUID id, SubjectType subjectType, UUID subjectId, UUID requesterId, Instant createdAt) {
        this.id = id;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.requesterId = requesterId;
        this.createdAt = createdAt;
    }

    void applyState(ApprovalStatus status, int currentStep, Instant completedAt) {
        this.status = status;
        this.currentStep = currentStep;
        this.completedAt = completedAt;
    }

    void addStep(ApprovalStepEntity step) {
        step.setInstance(this);
        steps.add(step);
    }

    UUID getId() { return id; }
    SubjectType getSubjectType() { return subjectType; }
    UUID getSubjectId() { return subjectId; }
    UUID getRequesterId() { return requesterId; }
    ApprovalStatus getStatus() { return status; }
    int getCurrentStep() { return currentStep; }
    Instant getCreatedAt() { return createdAt; }
    Instant getCompletedAt() { return completedAt; }
    Long getVersion() { return version; }
    List<ApprovalStepEntity> getSteps() { return steps; }
}