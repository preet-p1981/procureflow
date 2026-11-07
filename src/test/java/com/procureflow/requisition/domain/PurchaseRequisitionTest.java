package com.procureflow.requisition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseRequisitionTest {

    private static final Currency INR = Currency.getInstance("INR");
    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    private static RequisitionLine line(int no, String qty, String price) {
        return new RequisitionLine(no, null, "Ballpoint pen", new BigDecimal(qty), "box", Money.of(price, "INR"));
    }

    private PurchaseRequisition draft(RequisitionLine... lines) {
        return PurchaseRequisition.createDraft("PR-2026-000001", UUID.randomUUID(), UUID.randomUUID(),
                INR, "Team needs stationery", List.of(lines));
    }

    private void assertCode(Runnable action, RequisitionError code) {
        assertThatThrownBy(action::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode()).isEqualTo(code);
    }

    @Test
    void newRequisitionIsADraftWithTotal() {
        PurchaseRequisition r = draft(line(1, "10", "12.50"), line(2, "2", "100"));
        assertThat(r.status()).isEqualTo(RequisitionStatus.DRAFT);
        assertThat(r.total()).isEqualTo(Money.of("325", "INR"));      // 125 + 200
    }

    @Test
    void submitMovesToPendingApproval() {
        PurchaseRequisition r = draft(line(1, "1", "50"));
        r.submit(NOW);
        assertThat(r.status()).isEqualTo(RequisitionStatus.PENDING_APPROVAL);
        assertThat(r.submittedAt()).isEqualTo(NOW);
    }

    @Test
    void cannotSubmitWithoutLinesOrJustificationOrValue() {
        assertCode(() -> draft().submit(NOW), RequisitionError.INVALID_REQUISITION);
        assertCode(() -> PurchaseRequisition.createDraft("PR-1", UUID.randomUUID(), UUID.randomUUID(), INR,
                "  ", List.of(line(1, "1", "5"))).submit(NOW), RequisitionError.INVALID_REQUISITION);
        assertCode(() -> draft(line(1, "3", "0")).submit(NOW), RequisitionError.INVALID_REQUISITION);
    }

    @Test
    void onlyDraftsCanBeEditedOrSubmitted() {
        PurchaseRequisition r = draft(line(1, "1", "50"));
        r.submit(NOW);
        assertCode(() -> r.updateDraft("changed", List.of()), RequisitionError.INVALID_REQUISITION_STATE);
        assertCode(() -> r.submit(NOW), RequisitionError.INVALID_REQUISITION_STATE);
    }

    @Test
    void updateDraftReplacesJustificationAndLines() {
        PurchaseRequisition r = draft(line(1, "1", "50"));
        r.updateDraft("New reason", List.of(line(1, "4", "25")));
        assertThat(r.justification()).isEqualTo("New reason");
        assertThat(r.total()).isEqualTo(Money.of("100", "INR"));
    }

    @Test
    void approveAndRejectNeedPendingStatus() {
        PurchaseRequisition r = draft(line(1, "1", "50"));
        assertCode(() -> r.approve(NOW), RequisitionError.INVALID_REQUISITION_STATE);
        assertCode(() -> r.reject("no", NOW), RequisitionError.INVALID_REQUISITION_STATE);
        r.submit(NOW);
        r.approve(NOW);
        assertThat(r.status()).isEqualTo(RequisitionStatus.APPROVED);
        assertThat(r.holdsBudget()).isTrue();
    }

    @Test
    void rejectionNeedsReasonAndCanBeReopened() {
        PurchaseRequisition r = draft(line(1, "1", "50"));
        r.submit(NOW);
        assertCode(() -> r.reject(" ", NOW), RequisitionError.INVALID_REQUISITION);
        r.reject("Too expensive", NOW);
        assertThat(r.status()).isEqualTo(RequisitionStatus.REJECTED);
        assertThat(r.rejectionReason()).isEqualTo("Too expensive");

        r.reopen();
        assertThat(r.status()).isEqualTo(RequisitionStatus.DRAFT);
        assertThat(r.rejectionReason()).isNull();
        assertThat(r.holdsBudget()).isFalse();
    }

    @Test
    void cancelWorksUntilFinalAndOnlyApprovedHoldsBudget() {
        PurchaseRequisition r = draft(line(1, "1", "50"));
        r.submit(NOW);
        r.approve(NOW);
        assertThat(r.holdsBudget()).isTrue();
        r.cancel(NOW);
        assertThat(r.status()).isEqualTo(RequisitionStatus.CANCELLED);
        assertThat(r.holdsBudget()).isFalse();
        assertCode(() -> r.cancel(NOW), RequisitionError.INVALID_REQUISITION_STATE);
    }

    @Test
    void rejectsDuplicateLineNumbersAndWrongCurrency() {
        assertCode(() -> draft(line(1, "1", "5"), line(1, "2", "6")), RequisitionError.INVALID_REQUISITION);
        RequisitionLine usd = new RequisitionLine(1, null, "Cable", BigDecimal.ONE, "pc", Money.of("5", "USD"));
        assertCode(() -> draft(usd), RequisitionError.INVALID_REQUISITION);
    }

    @Test
    void rejectsTooManyLines() {
        RequisitionLine[] many = new RequisitionLine[PurchaseRequisition.MAX_LINES + 1];
        for (int i = 0; i < many.length; i++) {
            many[i] = line(i + 1, "1", "1");
        }
        assertCode(() -> draft(many), RequisitionError.INVALID_REQUISITION);
    }

    @Test
    void lineRejectsBadValues() {
        assertCode(() -> line(1, "0", "5"), RequisitionError.INVALID_REQUISITION);
        assertCode(() -> new RequisitionLine(1, null, " ", BigDecimal.ONE, "pc", Money.of("5", "INR")),
                RequisitionError.INVALID_REQUISITION);
        assertCode(() -> new RequisitionLine(1, null, "x", BigDecimal.ONE, "pc", Money.of("-5", "INR")),
                RequisitionError.INVALID_REQUISITION);
    }

    @Test
    void lineNormalizesUnitAndQuantity() {
        RequisitionLine l = new RequisitionLine(1, null, "  Cable ", new BigDecimal("2.5"), "pc", Money.of("5", "INR"));
        assertThat(l.unitOfMeasure()).isEqualTo("PC");
        assertThat(l.description()).isEqualTo("Cable");
        assertThat(l.lineTotal()).isEqualTo(Money.of("12.5", "INR"));
    }
}