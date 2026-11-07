package com.procureflow.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalPolicyTest {

    private final ApprovalPolicy policy = new ApprovalPolicy(List.of(
            new DepartmentManagerRule(),
            new AmountThresholdRule(Money.of("100000", "INR"), Money.of("500000", "INR"))));

    private ApprovalContext ctx(String amount, String currency, String... requesterRoles) {
        return new ApprovalContext(SubjectType.REQUISITION, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Money.of(amount, currency), Set.of(requesterRoles));
    }

    private List<String> roles(ApprovalContext c) {
        return policy.buildChain(c).stream().map(ApprovalStepSpec::approverRole).toList();
    }

    @Test
    void smallRequestNeedsOnlyTheManager() {
        assertThat(roles(ctx("5000", "INR", "REQUESTER"))).containsExactly("DEPARTMENT_MANAGER");
    }

    @Test
    void amountAtTheLimitDoesNotTriggerTheNextLevel() {
        assertThat(roles(ctx("100000", "INR", "REQUESTER"))).containsExactly("DEPARTMENT_MANAGER");
        assertThat(roles(ctx("100000.01", "INR", "REQUESTER")))
                .containsExactly("DEPARTMENT_MANAGER", "FINANCE_APPROVER");
    }

    @Test
    void veryLargeRequestNeedsThreeLevelsInOrder() {
        assertThat(roles(ctx("600000", "INR", "REQUESTER")))
                .containsExactly("DEPARTMENT_MANAGER", "FINANCE_APPROVER", "ADMIN");
    }

    @Test
    void managerRequestingSmallAmountFallsBackToFinance() {
        assertThat(roles(ctx("5000", "INR", "DEPARTMENT_MANAGER"))).containsExactly("FINANCE_APPROVER");
    }

    @Test
    void managerRequestingLargeAmountSkipsOnlyTheirOwnStep() {
        assertThat(roles(ctx("600000", "INR", "DEPARTMENT_MANAGER")))
                .containsExactly("FINANCE_APPROVER", "ADMIN");
    }

    @Test
    void foreignCurrencyIsTreatedAsAboveTheLimit() {
        assertThat(roles(ctx("10", "USD", "REQUESTER")))
                .containsExactly("DEPARTMENT_MANAGER", "FINANCE_APPROVER", "ADMIN");
    }

    @Test
    void stepsCarryTheirSlaHours() {
        List<ApprovalStepSpec> chain = policy.buildChain(ctx("200000", "INR", "REQUESTER"));
        assertThat(chain).extracting(ApprovalStepSpec::slaHours).containsExactly(24, 48);
    }

    @Test
    void aNewRuleCanBeAddedWithoutChangingExistingOnes() {
        ApprovalRule procurementForBigOrders = new ApprovalRule() {
            @Override public boolean appliesTo(ApprovalContext c) { return c.total().isGreaterThan(Money.of("50", "INR")); }
            @Override public List<ApprovalStepSpec> steps(ApprovalContext c) { return List.of(new ApprovalStepSpec("PROCUREMENT_OFFICER", 24)); }
            @Override public int order() { return 15; }          // between manager (10) and amount rule (20)
        };
        ApprovalPolicy extended = new ApprovalPolicy(List.of(
                new AmountThresholdRule(Money.of("100000", "INR"), Money.of("500000", "INR")),
                procurementForBigOrders,
                new DepartmentManagerRule()));                    // order of the list does not matter

        assertThat(extended.buildChain(ctx("200000", "INR", "REQUESTER")))
                .extracting(ApprovalStepSpec::approverRole)
                .containsExactly("DEPARTMENT_MANAGER", "PROCUREMENT_OFFICER", "FINANCE_APPROVER");
    }

    @Test
    void duplicateRolesAppearOnlyOnce() {
        ApprovalRule financeAgain = new ApprovalRule() {
            @Override public boolean appliesTo(ApprovalContext c) { return true; }
            @Override public List<ApprovalStepSpec> steps(ApprovalContext c) { return List.of(new ApprovalStepSpec("FINANCE_APPROVER", 1)); }
            @Override public int order() { return 30; }
        };
        ApprovalPolicy p = new ApprovalPolicy(List.of(
                new AmountThresholdRule(Money.of("100000", "INR"), Money.of("500000", "INR")), financeAgain));
        assertThat(p.buildChain(ctx("200000", "INR", "REQUESTER")))
                .extracting(ApprovalStepSpec::approverRole).containsExactly("FINANCE_APPROVER");
    }

    @Test
    void thresholdsMustBeConsistent() {
        assertThatThrownBy(() -> new AmountThresholdRule(Money.of("500", "INR"), Money.of("100", "INR")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new AmountThresholdRule(Money.of("100", "INR"), Money.of("500", "USD")))
                .isInstanceOf(DomainException.class);
    }
}