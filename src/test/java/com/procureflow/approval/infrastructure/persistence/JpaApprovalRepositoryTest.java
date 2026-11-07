package com.procureflow.approval.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.approval.application.InboxItem;
import com.procureflow.approval.application.port.ApprovalInboxQuery;
import com.procureflow.approval.domain.ApprovalError;
import com.procureflow.approval.domain.ApprovalInstance;
import com.procureflow.approval.domain.ApprovalStatus;
import com.procureflow.approval.domain.ApprovalStepSpec;
import com.procureflow.approval.domain.ApproverContext;
import com.procureflow.approval.domain.StepDecision;
import com.procureflow.approval.domain.SubjectType;
import com.procureflow.approval.domain.port.ApprovalRepository;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaApprovalRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    @Autowired ApprovalRepository approvals;
    @Autowired ApprovalInboxQuery inbox;
    @Autowired UserRepository users;
    @Autowired EntityManager em;

    private UUID requester;
    private ApproverContext manager;
    private ApproverContext finance;

    @BeforeEach
    void setUp() {
        requester = newUser();
        manager = new ApproverContext(newUser(), Set.of("DEPARTMENT_MANAGER"));
        finance = new ApproverContext(newUser(), Set.of("FINANCE_APPROVER"));
    }

    private UUID newUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return users.save(User.register(EmailAddress.of(unique + "@example.com"), "hash", "User", Set.of()))
                .id().value();
    }

    private ApprovalInstance newChain(UUID subjectId) {
        return approvals.save(ApprovalInstance.start(SubjectType.REQUISITION, subjectId, requester,
                List.of(new ApprovalStepSpec("DEPARTMENT_MANAGER", 24), new ApprovalStepSpec("FINANCE_APPROVER", 48)),
                NOW));
    }

    private ApprovalInstance reload(UUID id) {
        em.flush();
        em.clear();                       // force the next read to hit the database
        return approvals.findById(id).orElseThrow();
    }

    @Test
    void savesAndLoadsChainWithSteps() {
        ApprovalInstance saved = newChain(UUID.randomUUID());

        ApprovalInstance loaded = reload(saved.id());
        assertThat(loaded.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(loaded.currentStepNo()).isEqualTo(1);
        assertThat(loaded.steps()).hasSize(2);
        assertThat(loaded.steps().get(0).approverRole()).isEqualTo("DEPARTMENT_MANAGER");
        assertThat(loaded.steps().get(1).slaDueAt()).isEqualTo(NOW.plusSeconds(48 * 3600L));
        assertThat(loaded.requesterId()).isEqualTo(requester);
    }

    @Test
    void persistsDecisionsAndProgress() {
        ApprovalInstance a = newChain(UUID.randomUUID());
        a.approve(manager, "Looks fine", NOW.plusSeconds(60));
        approvals.save(a);

        ApprovalInstance afterFirst = reload(a.id());
        assertThat(afterFirst.currentStepNo()).isEqualTo(2);
        assertThat(afterFirst.steps().get(0).decision()).isEqualTo(StepDecision.APPROVED);
        assertThat(afterFirst.steps().get(0).decidedBy()).isEqualTo(manager.userId());
        assertThat(afterFirst.steps().get(0).comment()).isEqualTo("Looks fine");

        afterFirst.approve(finance, null, NOW.plusSeconds(120));
        approvals.save(afterFirst);

        ApprovalInstance done = reload(a.id());
        assertThat(done.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(done.completedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectionKeepsTheHistoryIncludingSkippedSteps() {
        ApprovalInstance a = newChain(UUID.randomUUID());
        a.reject(manager, "Too expensive", NOW);
        approvals.save(a);

        ApprovalInstance loaded = reload(a.id());
        assertThat(loaded.status()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(loaded.steps().get(0).decision()).isEqualTo(StepDecision.REJECTED);
        assertThat(loaded.steps().get(1).decision()).isEqualTo(StepDecision.SKIPPED);
    }

    @Test
    void findsOnlyTheActiveChainOfASubject() {
        UUID subject = UUID.randomUUID();
        ApprovalInstance a = newChain(subject);
        assertThat(approvals.findActiveBySubject(SubjectType.REQUISITION, subject)).isPresent();

        a.cancel(NOW);
        approvals.save(a);
        em.flush();
        em.clear();
        assertThat(approvals.findActiveBySubject(SubjectType.REQUISITION, subject)).isEmpty();
    }

    @Test
    void onlyOneActiveChainPerSubject() {
        UUID subject = UUID.randomUUID();
        newChain(subject);
        assertThatThrownBy(() -> newChain(subject))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(ApprovalError.INVALID_APPROVAL_STATE);
    }

    @Test
    void staleVersionIsRejected() {
        ApprovalInstance saved = newChain(UUID.randomUUID());
        ApprovalInstance copyA = approvals.findById(saved.id()).orElseThrow();
        ApprovalInstance copyB = approvals.findById(saved.id()).orElseThrow();

        copyA.approve(manager, null, NOW);
        approvals.save(copyA);

        ApproverContext otherManager = new ApproverContext(newUser(), Set.of("DEPARTMENT_MANAGER"));
        copyB.approve(otherManager, null, NOW);
        assertThatThrownBy(() -> approvals.save(copyB))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CommonError.CONFLICT);
    }

    @Test
    void inboxShowsOnlyTheCurrentStepForMatchingRolesAndNotOwnRequests() {
        UUID subject = UUID.randomUUID();
        ApprovalInstance a = newChain(subject);

        assertThat(subjects(inbox.pending(Set.of("DEPARTMENT_MANAGER"), manager.userId(), 0, 50))).contains(subject);
        assertThat(subjects(inbox.pending(Set.of("FINANCE_APPROVER"), finance.userId(), 0, 50))).doesNotContain(subject);
        assertThat(subjects(inbox.pending(Set.of("DEPARTMENT_MANAGER"), requester, 0, 50))).doesNotContain(subject);
        assertThat(inbox.pending(Set.of(), manager.userId(), 0, 50).content()).isEmpty();

        a.approve(manager, null, NOW);
        approvals.save(a);
        em.flush();
        em.clear();

        assertThat(subjects(inbox.pending(Set.of("DEPARTMENT_MANAGER"), manager.userId(), 0, 50))).doesNotContain(subject);
        assertThat(subjects(inbox.pending(Set.of("FINANCE_APPROVER"), finance.userId(), 0, 50))).contains(subject);
    }

    private static List<UUID> subjects(com.procureflow.shared.application.PageResult<InboxItem> page) {
        return page.content().stream().map(InboxItem::subjectId).toList();
    }
}