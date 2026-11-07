package com.procureflow.approval.domain;

import com.procureflow.shared.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ApprovalInstance {

    public static final int MAX_STEPS = 10;

    private final UUID id;
    private final SubjectType subjectType;
    private final UUID subjectId;
    private final UUID requesterId;
    private final Instant createdAt;
    private final long version;
    private final List<ApprovalStep> steps = new ArrayList<>();

    private ApprovalStatus status;
    private int currentStepNo;
    private Instant completedAt;

    private ApprovalInstance(UUID id, SubjectType subjectType, UUID subjectId, UUID requesterId,
                             ApprovalStatus status, int currentStepNo, List<ApprovalStep> steps,
                             Instant createdAt, Instant completedAt, long version) {
        this.id = Objects.requireNonNull(id);
        this.subjectType = Objects.requireNonNull(subjectType);
        this.subjectId = Objects.requireNonNull(subjectId);
        this.requesterId = Objects.requireNonNull(requesterId);
        this.status = Objects.requireNonNull(status);
        this.currentStepNo = currentStepNo;
        this.steps.addAll(steps);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.completedAt = completedAt;
        this.version = version;
    }

    /** Starts a chain. The first step is immediately the current one. */
    public static ApprovalInstance start(SubjectType subjectType, UUID subjectId, UUID requesterId,
                                         List<ApprovalStepSpec> specs, Instant now) {
        if (specs == null || specs.isEmpty()) {
            throw new DomainException(ApprovalError.INVALID_APPROVAL, "At least one approval step is required");
        }
        if (specs.size() > MAX_STEPS) {
            throw new DomainException(ApprovalError.INVALID_APPROVAL, "At most " + MAX_STEPS + " steps are allowed");
        }
        List<ApprovalStep> steps = new ArrayList<>();
        int no = 1;
        for (ApprovalStepSpec spec : specs) {
            steps.add(ApprovalStep.pending(no++, spec.approverRole(), now.plus(Duration.ofHours(spec.slaHours()))));
        }
        return new ApprovalInstance(UUID.randomUUID(), subjectType, subjectId, requesterId,
                ApprovalStatus.PENDING, 1, steps, now, null, 0L);
    }

    public static ApprovalInstance reconstitute(UUID id, SubjectType subjectType, UUID subjectId,
                                                UUID requesterId, ApprovalStatus status, int currentStepNo,
                                                List<ApprovalStep> steps, Instant createdAt,
                                                Instant completedAt, long version) {
        return new ApprovalInstance(id, subjectType, subjectId, requesterId, status, currentStepNo,
                steps, createdAt, completedAt, version);
    }

    // ---- behaviour ----

    public void approve(ApproverContext approver, String comment, Instant now) {
        ApprovalStep step = stepToDecide(approver);
        step.approve(approver.userId(), comment, now);
        if (currentStepNo == steps.size()) {
            status = ApprovalStatus.APPROVED;
            completedAt = now;
        } else {
            currentStepNo++;
        }
    }

    public void reject(ApproverContext approver, String comment, Instant now) {
        if (comment == null || comment.isBlank()) {
            throw new DomainException(ApprovalError.INVALID_APPROVAL, "A comment is required to reject");
        }
        ApprovalStep step = stepToDecide(approver);
        step.reject(approver.userId(), comment, now);
        steps.forEach(ApprovalStep::skip);        // later steps are no longer needed
        status = ApprovalStatus.REJECTED;
        completedAt = now;
    }

    /** Used when the subject (the requisition) is cancelled while approval is still open. */
    public void cancel(Instant now) {
        requirePending("cancel");
        steps.forEach(ApprovalStep::skip);
        status = ApprovalStatus.CANCELLED;
        completedAt = now;
    }

    public Optional<ApprovalStep> currentStep() {
        return status == ApprovalStatus.PENDING
                ? Optional.of(steps.get(currentStepNo - 1))
                : Optional.empty();
    }

    // ---- read access ----

    public UUID id() { return id; }
    public SubjectType subjectType() { return subjectType; }
    public UUID subjectId() { return subjectId; }
    public UUID requesterId() { return requesterId; }
    public ApprovalStatus status() { return status; }
    public int currentStepNo() { return currentStepNo; }
    public List<ApprovalStep> steps() { return List.copyOf(steps); }
    public Instant createdAt() { return createdAt; }
    public Instant completedAt() { return completedAt; }
    public long version() { return version; }

    // ---- helpers ----

    private ApprovalStep stepToDecide(ApproverContext approver) {
        requirePending("decide");
        ApprovalStep step = steps.get(currentStepNo - 1);

        if (approver.userId().equals(requesterId)) {
            throw new DomainException(ApprovalError.SELF_APPROVAL_NOT_ALLOWED,
                    "You cannot decide your own request");
        }
        if (!approver.roles().contains(step.approverRole())) {
            throw new DomainException(ApprovalError.NOT_AUTHORIZED_APPROVER,
                    "This step needs the role " + step.approverRole());
        }
        boolean alreadySigned = steps.stream().limit(currentStepNo - 1L)
                .anyMatch(s -> approver.userId().equals(s.decidedBy()));
        if (alreadySigned) {
            throw new DomainException(ApprovalError.APPROVER_ALREADY_SIGNED,
                    "You already approved an earlier step of this request");
        }
        return step;
    }

    private void requirePending(String action) {
        if (status != ApprovalStatus.PENDING) {
            throw new DomainException(ApprovalError.INVALID_APPROVAL_STATE,
                    "Cannot " + action + " an approval that is " + status);
        }
    }
}