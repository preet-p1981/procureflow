package com.procureflow.approval.domain;

import java.time.Instant;
import java.util.UUID;

public class ApprovalStep {

    private final int stepNo;
    private final String approverRole;
    private final Instant slaDueAt;
    private StepDecision decision;
    private UUID decidedBy;
    private Instant decidedAt;
    private String comment;

    private ApprovalStep(int stepNo, String approverRole, Instant slaDueAt, StepDecision decision,
                         UUID decidedBy, Instant decidedAt, String comment) {
        this.stepNo = stepNo;
        this.approverRole = approverRole;
        this.slaDueAt = slaDueAt;
        this.decision = decision;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
        this.comment = comment;
    }

    static ApprovalStep pending(int stepNo, String approverRole, Instant slaDueAt) {
        return new ApprovalStep(stepNo, approverRole, slaDueAt, StepDecision.PENDING, null, null, null);
    }

    public static ApprovalStep reconstitute(int stepNo, String approverRole, Instant slaDueAt,
                                            StepDecision decision, UUID decidedBy, Instant decidedAt,
                                            String comment) {
        return new ApprovalStep(stepNo, approverRole, slaDueAt, decision, decidedBy, decidedAt, comment);
    }

    // only the ApprovalInstance (same package) may change a step
    void approve(UUID by, String comment, Instant now) {
        decide(StepDecision.APPROVED, by, comment, now);
    }

    void reject(UUID by, String comment, Instant now) {
        decide(StepDecision.REJECTED, by, comment, now);
    }

    void skip() {
        if (decision == StepDecision.PENDING) {
            decision = StepDecision.SKIPPED;
        }
    }

    private void decide(StepDecision result, UUID by, String note, Instant now) {
        this.decision = result;
        this.decidedBy = by;
        this.decidedAt = now;
        this.comment = note == null || note.isBlank() ? null : note.trim();
    }

    public int stepNo() { return stepNo; }
    public String approverRole() { return approverRole; }
    public Instant slaDueAt() { return slaDueAt; }
    public StepDecision decision() { return decision; }
    public UUID decidedBy() { return decidedBy; }
    public Instant decidedAt() { return decidedAt; }
    public String comment() { return comment; }
}