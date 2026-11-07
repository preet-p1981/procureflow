package com.procureflow.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalInstanceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    private final UUID requester = UUID.randomUUID();
    private final ApproverContext manager = new ApproverContext(UUID.randomUUID(), Set.of("DEPARTMENT_MANAGER"));
    private final ApproverContext finance = new ApproverContext(UUID.randomUUID(), Set.of("FINANCE_APPROVER"));

    private ApprovalInstance twoStepChain() {
        return ApprovalInstance.start(SubjectType.REQUISITION, UUID.randomUUID(), requester,
                List.of(new ApprovalStepSpec("department_manager", 24), new ApprovalStepSpec("FINANCE_APPROVER", 48)), NOW);
    }

    private void assertCode(Runnable action, ApprovalError code) {
        assertThatThrownBy(action::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode()).isEqualTo(code);
    }

    @Test
    void startsPendingAtFirstStepWithSlaDeadlines() {
        ApprovalInstance a = twoStepChain();
        assertThat(a.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(a.currentStepNo()).isEqualTo(1);
        assertThat(a.currentStep().orElseThrow().approverRole()).isEqualTo("DEPARTMENT_MANAGER");
        assertThat(a.steps().get(0).slaDueAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(a.steps().get(1).slaDueAt()).isEqualTo(NOW.plus(Duration.ofHours(48)));
    }

    @Test
    void approvingAllStepsInOrderCompletesTheChain() {
        ApprovalInstance a = twoStepChain();
        a.approve(manager, "ok", NOW);
        assertThat(a.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(a.currentStepNo()).isEqualTo(2);

        a.approve(finance, null, NOW.plusSeconds(60));
        assertThat(a.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(a.completedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(a.currentStep()).isEmpty();
        assertThat(a.steps()).allMatch(s -> s.decision() == StepDecision.APPROVED);
    }

    @Test
    void rejectionNeedsCommentEndsChainAndSkipsTheRest() {
        ApprovalInstance a = twoStepChain();
        assertCode(() -> a.reject(manager, " ", NOW), ApprovalError.INVALID_APPROVAL);

        a.reject(manager, "Over budget", NOW);
        assertThat(a.status()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(a.steps().get(0).decision()).isEqualTo(StepDecision.REJECTED);
        assertThat(a.steps().get(0).comment()).isEqualTo("Over budget");
        assertThat(a.steps().get(1).decision()).isEqualTo(StepDecision.SKIPPED);
    }

    @Test
    void requesterCannotDecideOwnRequest() {
        ApprovalInstance a = twoStepChain();
        ApproverContext self = new ApproverContext(requester, Set.of("DEPARTMENT_MANAGER"));
        assertCode(() -> a.approve(self, null, NOW), ApprovalError.SELF_APPROVAL_NOT_ALLOWED);
        assertCode(() -> a.reject(self, "no", NOW), ApprovalError.SELF_APPROVAL_NOT_ALLOWED);
    }

    @Test
    void onlyTheRoleOfTheCurrentStepMayDecide() {
        ApprovalInstance a = twoStepChain();
        assertCode(() -> a.approve(finance, null, NOW), ApprovalError.NOT_AUTHORIZED_APPROVER);   // step 1 needs a manager
    }

    @Test
    void samePersonCannotSignTwoSteps() {
        ApprovalInstance a = twoStepChain();
        ApproverContext both = new ApproverContext(UUID.randomUUID(), Set.of("DEPARTMENT_MANAGER", "FINANCE_APPROVER"));
        a.approve(both, null, NOW);
        assertCode(() -> a.approve(both, null, NOW), ApprovalError.APPROVER_ALREADY_SIGNED);
        assertThat(a.status()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void cannotDecideAfterCompletion() {
        ApprovalInstance a = twoStepChain();
        a.reject(manager, "no", NOW);
        assertCode(() -> a.approve(manager, null, NOW), ApprovalError.INVALID_APPROVAL_STATE);
        assertCode(() -> a.cancel(NOW), ApprovalError.INVALID_APPROVAL_STATE);
    }

    @Test
    void cancelSkipsRemainingSteps() {
        ApprovalInstance a = twoStepChain();
        a.approve(manager, null, NOW);
        a.cancel(NOW.plusSeconds(5));
        assertThat(a.status()).isEqualTo(ApprovalStatus.CANCELLED);
        assertThat(a.steps().get(0).decision()).isEqualTo(StepDecision.APPROVED);   // history is kept
        assertThat(a.steps().get(1).decision()).isEqualTo(StepDecision.SKIPPED);
    }

    @Test
    void rejectsEmptyOrTooLongChainsAndBadSpecs() {
        UUID subject = UUID.randomUUID();
        assertCode(() -> ApprovalInstance.start(SubjectType.REQUISITION, subject, requester, List.of(), NOW),
                ApprovalError.INVALID_APPROVAL);
        List<ApprovalStepSpec> tooMany = java.util.Collections.nCopies(ApprovalInstance.MAX_STEPS + 1,
                new ApprovalStepSpec("ADMIN", 1));
        assertCode(() -> ApprovalInstance.start(SubjectType.REQUISITION, subject, requester, tooMany, NOW),
                ApprovalError.INVALID_APPROVAL);
        assertCode(() -> new ApprovalStepSpec(" ", 1), ApprovalError.INVALID_APPROVAL);
        assertCode(() -> new ApprovalStepSpec("ADMIN", 0), ApprovalError.INVALID_APPROVAL);
    }
}