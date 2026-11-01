package com.procureflow.catalog.domain;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;

/**
 * Money lifecycle: allocated budget is first committed (requisition approved),
 * then either released (cancelled) or consumed (invoice posted).
 */
public class Budget {

    private final UUID id;
    private final UUID departmentId;
    private final int fiscalYear;
    private final Money allocated;
    private final long version;

    private Money committed;
    private Money consumed;

    private Budget(UUID id, UUID departmentId, int fiscalYear, Money allocated,
                   Money committed, Money consumed, long version) {
        this.id = Objects.requireNonNull(id);
        this.departmentId = Objects.requireNonNull(departmentId);
        if (fiscalYear < 2000 || fiscalYear > 2100) {
            throw new DomainException(CatalogError.INVALID_BUDGET, "Fiscal year out of range");
        }
        this.fiscalYear = fiscalYear;
        this.allocated = Objects.requireNonNull(allocated);
        if (allocated.isNegative()) {
            throw new DomainException(CatalogError.INVALID_BUDGET, "Allocated amount must not be negative");
        }
        this.committed = Objects.requireNonNull(committed);
        this.consumed = Objects.requireNonNull(consumed);
        this.version = version;
    }

    public static Budget create(UUID departmentId, int fiscalYear, Money allocated) {
        Money zero = Money.zero(allocated.currency());
        return new Budget(UUID.randomUUID(), departmentId, fiscalYear, allocated, zero, zero, 0L);
    }

    public static Budget reconstitute(UUID id, UUID departmentId, int fiscalYear, Money allocated,
                                      Money committed, Money consumed, long version) {
        return new Budget(id, departmentId, fiscalYear, allocated, committed, consumed, version);
    }

    public Money available() {
        return allocated.subtract(committed).subtract(consumed);
    }

    /** Reserve money, for example when a requisition is approved. */
    public void commit(Money amount) {
        requirePositive(amount);
        if (amount.isGreaterThan(available())) {
            throw new DomainException(CatalogError.BUDGET_EXCEEDED,
                    "Budget exceeded: requested " + amount.amount() + ", available " + available().amount());
        }
        committed = committed.add(amount);
    }

    /** Give reserved money back, for example when a requisition is cancelled. */
    public void release(Money amount) {
        requirePositive(amount);
        requireWithinCommitted(amount, "release");
        committed = committed.subtract(amount);
    }

    /** Turn reserved money into spent money, for example when an invoice is posted. */
    public void consume(Money amount) {
        requirePositive(amount);
        requireWithinCommitted(amount, "consume");
        committed = committed.subtract(amount);
        consumed = consumed.add(amount);
    }

    private void requireWithinCommitted(Money amount, String action) {
        if (amount.isGreaterThan(committed)) {
            throw new DomainException(CatalogError.INVALID_BUDGET,
                    "Cannot " + action + " more than the committed amount");
        }
    }

    private static void requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new DomainException(CatalogError.INVALID_BUDGET, "Amount must be greater than zero");
        }
    }

    public UUID id() { return id; }
    public UUID departmentId() { return departmentId; }
    public int fiscalYear() { return fiscalYear; }
    public Money allocated() { return allocated; }
    public Money committed() { return committed; }
    public Money consumed() { return consumed; }
    public long version() { return version; }
}