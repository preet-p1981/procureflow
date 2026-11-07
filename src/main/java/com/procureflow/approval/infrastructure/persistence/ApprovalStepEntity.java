package com.procureflow.approval.infrastructure.persistence;

import com.procureflow.approval.domain.StepDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_step")
public class ApprovalStepEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false)
    private ApprovalInstanceEntity instance;

    @Column(name = "step_no", nullable = false, updatable = false)
    private int stepNo;

    @Column(name = "approver_role", nullable = false, length = 40, updatable = false)
    private String approverRole;

    @Column(name = "sla_due_at", nullable = false, updatable = false)
    private Instant slaDueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private StepDecision decision;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "comment", length = 500)
    private String comment;

    protected ApprovalStepEntity() {
    }

    ApprovalStepEntity(int stepNo, String approverRole, Instant slaDueAt) {
        this.id = UUID.randomUUID();
        this.stepNo = stepNo;
        this.approverRole = approverRole;
        this.slaDueAt = slaDueAt;
        this.decision = StepDecision.PENDING;
    }

    void applyDecision(StepDecision decision, UUID decidedBy, Instant decidedAt, String comment) {
        this.decision = decision;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
        this.comment = comment;
    }

    void setInstance(ApprovalInstanceEntity instance) { this.instance = instance; }

    int getStepNo() { return stepNo; }
    String getApproverRole() { return approverRole; }
    Instant getSlaDueAt() { return slaDueAt; }
    StepDecision getDecision() { return decision; }
    UUID getDecidedBy() { return decidedBy; }
    Instant getDecidedAt() { return decidedAt; }
    String getComment() { return comment; }
}