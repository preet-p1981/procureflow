package com.procureflow.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BudgetTest {

    private static Money inr(String amount) {
        return Money.of(amount, "INR");
    }

    private Budget budget() {
        return Budget.create(UUID.randomUUID(), 2026, inr("1000"));
    }

    private void assertError(Runnable action, Object expectedCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(expectedCode);
    }

    @Test
    void newBudgetIsFullyAvailable() {
        Budget b = budget();
        assertThat(b.available()).isEqualTo(inr("1000"));
        assertThat(b.committed().isZero()).isTrue();
    }

    @Test
    void commitReducesAvailable() {
        Budget b = budget();
        b.commit(inr("300"));
        assertThat(b.available()).isEqualTo(inr("700"));
        assertThat(b.committed()).isEqualTo(inr("300"));
    }

    @Test
    void canCommitExactlyTheAvailableAmount() {
        Budget b = budget();
        b.commit(inr("1000"));
        assertThat(b.available().isZero()).isTrue();
    }

    @Test
    void commitBeyondAvailableIsRejected() {
        Budget b = budget();
        b.commit(inr("900"));
        assertError(() -> b.commit(inr("101")), CatalogError.BUDGET_EXCEEDED);
        assertThat(b.committed()).isEqualTo(inr("900"));   // unchanged after the failure
    }

    @Test
    void releaseGivesMoneyBack() {
        Budget b = budget();
        b.commit(inr("400"));
        b.release(inr("150"));
        assertThat(b.committed()).isEqualTo(inr("250"));
        assertThat(b.available()).isEqualTo(inr("750"));
    }

    @Test
    void cannotReleaseMoreThanCommitted() {
        Budget b = budget();
        b.commit(inr("100"));
        assertError(() -> b.release(inr("101")), CatalogError.INVALID_BUDGET);
    }

    @Test
    void consumeMovesCommittedToConsumed() {
        Budget b = budget();
        b.commit(inr("500"));
        b.consume(inr("200"));
        assertThat(b.committed()).isEqualTo(inr("300"));
        assertThat(b.consumed()).isEqualTo(inr("200"));
        assertThat(b.available()).isEqualTo(inr("500"));
    }

    @Test
    void cannotConsumeMoreThanCommitted() {
        Budget b = budget();
        b.commit(inr("100"));
        assertError(() -> b.consume(inr("150")), CatalogError.INVALID_BUDGET);
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        Budget b = budget();
        assertError(() -> b.commit(inr("0")), CatalogError.INVALID_BUDGET);
        assertError(() -> b.commit(inr("-5")), CatalogError.INVALID_BUDGET);
    }

    @Test
    void rejectsDifferentCurrency() {
        Budget b = budget();
        assertError(() -> b.commit(Money.of("10", "USD")), CommonError.CURRENCY_MISMATCH);
    }

    @Test
    void rejectsNegativeAllocationAndBadYear() {
        UUID dept = UUID.randomUUID();
        assertError(() -> Budget.create(dept, 2026, inr("-1")), CatalogError.INVALID_BUDGET);
        assertError(() -> Budget.create(dept, 1999, inr("100")), CatalogError.INVALID_BUDGET);
    }
}