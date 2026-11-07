package com.procureflow.requisition.domain;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PurchaseRequisition {

    public static final int MAX_LINES = 100;

    private final UUID id;
    private final String number;
    private final UUID requesterId;
    private final UUID departmentId;
    private final Currency currency;
    private final long version;

    private String justification;
    private final List<RequisitionLine> lines = new ArrayList<>();
    private RequisitionStatus status;
    private String rejectionReason;
    private Instant submittedAt;
    private Instant decidedAt;

    private PurchaseRequisition(UUID id, String number, UUID requesterId, UUID departmentId, Currency currency,
                                String justification, List<RequisitionLine> lines, RequisitionStatus status,
                                String rejectionReason, Instant submittedAt, Instant decidedAt, long version) {
        this.id = Objects.requireNonNull(id);
        this.number = Objects.requireNonNull(number);
        this.requesterId = Objects.requireNonNull(requesterId);
        this.departmentId = Objects.requireNonNull(departmentId);
        this.currency = Objects.requireNonNull(currency);
        this.justification = justification == null ? "" : justification.trim();
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
        this.decidedAt = decidedAt;
        this.version = version;
        this.lines.addAll(checkLines(lines));
    }

    /** A new requisition always starts as a draft. */
    public static PurchaseRequisition createDraft(String number, UUID requesterId, UUID departmentId,
                                                  Currency currency, String justification,
                                                  List<RequisitionLine> lines) {
        return new PurchaseRequisition(UUID.randomUUID(), number, requesterId, departmentId, currency,
                justification, lines, RequisitionStatus.DRAFT, null, null, null, 0L);
    }

    public static PurchaseRequisition reconstitute(UUID id, String number, UUID requesterId, UUID departmentId,
                                                   Currency currency, String justification,
                                                   List<RequisitionLine> lines, RequisitionStatus status,
                                                   String rejectionReason, Instant submittedAt,
                                                   Instant decidedAt, long version) {
        return new PurchaseRequisition(id, number, requesterId, departmentId, currency, justification,
                lines, status, rejectionReason, submittedAt, decidedAt, version);
    }

    // ---- behaviour ----

    public void updateDraft(String newJustification, List<RequisitionLine> newLines) {
        requireStatus("edit", RequisitionStatus.DRAFT);
        List<RequisitionLine> checked = checkLines(newLines);
        this.justification = newJustification == null ? "" : newJustification.trim();
        this.lines.clear();
        this.lines.addAll(checked);
    }

    public void submit(Instant now) {
        requireStatus("submit", RequisitionStatus.DRAFT);
        if (justification.isBlank()) {
            throw invalid("A justification is required to submit");
        }
        if (lines.isEmpty()) {
            throw invalid("At least one line is required to submit");
        }
        if (!total().isPositive()) {
            throw invalid("Total must be greater than zero to submit");
        }
        status = RequisitionStatus.PENDING_APPROVAL;
        submittedAt = now;
    }

    public void approve(Instant now) {
        requireStatus("approve", RequisitionStatus.PENDING_APPROVAL);
        status = RequisitionStatus.APPROVED;
        decidedAt = now;
    }

    public void reject(String reason, Instant now) {
        requireStatus("reject", RequisitionStatus.PENDING_APPROVAL);
        if (reason == null || reason.isBlank()) {
            throw invalid("A rejection reason is required");
        }
        status = RequisitionStatus.REJECTED;
        rejectionReason = reason.trim();
        decidedAt = now;
    }

    /** A rejected requisition goes back to draft so the requester can fix and resubmit it. */
    public void reopen() {
        requireStatus("reopen", RequisitionStatus.REJECTED);
        status = RequisitionStatus.DRAFT;
        rejectionReason = null;
        submittedAt = null;
        decidedAt = null;
    }

    public void cancel(Instant now) {
        requireStatus("cancel", RequisitionStatus.DRAFT, RequisitionStatus.PENDING_APPROVAL,
                RequisitionStatus.APPROVED, RequisitionStatus.REJECTED);
        status = RequisitionStatus.CANCELLED;
        decidedAt = now;
    }

    public Money total() {
        Money sum = Money.zero(currency);
        for (RequisitionLine line : lines) {
            sum = sum.add(line.lineTotal());
        }
        return sum;
    }

    /** True while this requisition is holding budget, so cancelling it must release that budget. */
    public boolean holdsBudget() {
        return status == RequisitionStatus.APPROVED;
    }

    // ---- read access ----

    public UUID id() { return id; }
    public String number() { return number; }
    public UUID requesterId() { return requesterId; }
    public UUID departmentId() { return departmentId; }
    public Currency currency() { return currency; }
    public String justification() { return justification; }
    public List<RequisitionLine> lines() { return List.copyOf(lines); }
    public RequisitionStatus status() { return status; }
    public String rejectionReason() { return rejectionReason; }
    public Instant submittedAt() { return submittedAt; }
    public Instant decidedAt() { return decidedAt; }
    public long version() { return version; }

    // ---- helpers ----

    private List<RequisitionLine> checkLines(List<RequisitionLine> candidate) {
        List<RequisitionLine> list = candidate == null ? List.of() : candidate;
        if (list.size() > MAX_LINES) {
            throw invalid("A requisition can have at most " + MAX_LINES + " lines");
        }
        Set<Integer> seen = new HashSet<>();
        for (RequisitionLine line : list) {
            if (!seen.add(line.lineNo())) {
                throw invalid("Duplicate line number " + line.lineNo());
            }
            if (!line.unitPrice().currency().equals(currency)) {
                throw invalid("Line " + line.lineNo() + " must be in " + currency.getCurrencyCode());
            }
        }
        return List.copyOf(list);
    }

    private void requireStatus(String action, RequisitionStatus... allowed) {
        for (RequisitionStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new DomainException(RequisitionError.INVALID_REQUISITION_STATE,
                "Cannot " + action + " a requisition that is " + status);
    }

    private static DomainException invalid(String message) {
        return new DomainException(RequisitionError.INVALID_REQUISITION, message);
    }
}