package com.procureflow.requisition.application;

import com.procureflow.catalog.application.ItemView;
import com.procureflow.catalog.application.port.BudgetStore;
import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.application.port.ItemStore;
import com.procureflow.catalog.domain.Budget;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.requisition.application.port.CurrentUser;
import com.procureflow.requisition.domain.PurchaseRequisition;
import com.procureflow.requisition.domain.RequisitionError;
import com.procureflow.requisition.domain.RequisitionLine;
import com.procureflow.requisition.domain.port.RequisitionNumberGenerator;
import com.procureflow.requisition.domain.port.RequisitionRepository;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequisitionService {

    private final RequisitionRepository requisitions;
    private final RequisitionNumberGenerator numbers;
    private final CurrentUser currentUser;
    private final DepartmentStore departments;
    private final ItemStore items;
    private final BudgetStore budgets;
    private final Clock clock;

    public RequisitionService(RequisitionRepository requisitions, RequisitionNumberGenerator numbers,
                              CurrentUser currentUser, DepartmentStore departments, ItemStore items,
                              BudgetStore budgets, ObjectProvider<Clock> clock) {
        this.requisitions = requisitions;
        this.numbers = numbers;
        this.currentUser = currentUser;
        this.departments = departments;
        this.items = items;
        this.budgets = budgets;
        this.clock = clock.getIfAvailable(Clock::systemUTC);   // uses your Clock bean if you have one
    }

    @Transactional
    @PreAuthorize("hasAuthority('REQ_CREATE')")
    public RequisitionView createDraft(CreateRequisitionCommand cmd) {
        Currency currency = parseCurrency(cmd.currency());
        departments.findById(cmd.departmentId()).orElseThrow(() ->
                new DomainException(RequisitionError.INVALID_REQUISITION, "Unknown department"));
        List<RequisitionLine> lines = toLines(cmd.lines(), currency);

        Instant now = clock.instant();
        String number = numbers.next(yearOf(now));
        PurchaseRequisition draft = PurchaseRequisition.createDraft(number, currentUser.id(),
                cmd.departmentId(), currency, cmd.justification(), lines);
        return RequisitionView.from(requisitions.save(draft));
    }

    @Transactional
    @PreAuthorize("hasAuthority('REQ_CREATE')")
    public RequisitionView updateDraft(UUID id, String justification, List<RequisitionLineInput> lineInputs) {
        PurchaseRequisition r = loadOwned(id);
        r.updateDraft(justification, toLines(lineInputs, r.currency()));
        return RequisitionView.from(requisitions.save(r));
    }

    @Transactional
    @PreAuthorize("hasAuthority('REQ_CREATE')")
    public RequisitionView submit(UUID id) {
        PurchaseRequisition r = loadOwned(id);
        r.submit(clock.instant());
        return RequisitionView.from(requisitions.save(r));
    }

    @Transactional
    @PreAuthorize("hasAuthority('REQ_CREATE')")
    public RequisitionView reopen(UUID id) {
        PurchaseRequisition r = loadOwned(id);
        r.reopen();
        return RequisitionView.from(requisitions.save(r));
    }

    @Transactional
    @PreAuthorize("hasAuthority('REQ_CREATE')")
    public RequisitionView cancel(UUID id) {
        PurchaseRequisition r = loadOwned(id);

        // read what we need BEFORE cancelling: cancel() overwrites decidedAt
        boolean heldBudget = r.holdsBudget();
        int budgetYear = heldBudget ? yearOf(r.decidedAt()) : 0;

        r.cancel(clock.instant());
        PurchaseRequisition saved = requisitions.save(r);

        if (heldBudget) {
            // same transaction: if releasing fails, the cancellation is rolled back too
            Budget budget = budgets.find(r.departmentId(), budgetYear).orElseThrow(() ->
                    new DomainException(CatalogError.BUDGET_NOT_FOUND, "Budget not found"));
            budgets.release(budget.id(), r.total());
        }
        return RequisitionView.from(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('REQ_READ_OWN', 'REQ_READ_ALL')")
    public RequisitionView get(UUID id) {
        PurchaseRequisition r = requisitions.findById(id).orElseThrow(this::notFound);
        boolean owner = r.requesterId().equals(currentUser.id());
        if (!owner && !currentUser.hasAuthority("REQ_READ_ALL")) {
            throw notFound();                       // do not reveal that someone else's requisition exists
        }
        return RequisitionView.from(r);
    }

    // ---- helpers ----

    private PurchaseRequisition loadOwned(UUID id) {
        PurchaseRequisition r = requisitions.findById(id).orElseThrow(this::notFound);
        if (!r.requesterId().equals(currentUser.id())) {
            throw notFound();
        }
        return r;
    }

    private List<RequisitionLine> toLines(List<RequisitionLineInput> inputs, Currency currency) {
        List<RequisitionLine> lines = new ArrayList<>();
        if (inputs == null) {
            return lines;
        }
        int lineNo = 1;
        for (RequisitionLineInput in : inputs) {
            if (in.unitPrice() == null) {
                throw new DomainException(RequisitionError.INVALID_REQUISITION,
                        "Line " + lineNo + ": unit price is required");
            }
            if (in.itemId() != null) {
                ItemView item = items.findById(in.itemId()).orElseThrow(() ->
                        new DomainException(RequisitionError.INVALID_REQUISITION, "Unknown item"));
                if (!item.active()) {
                    throw new DomainException(RequisitionError.INVALID_REQUISITION,
                            "Item " + item.sku() + " is not active");
                }
            }
            lines.add(new RequisitionLine(lineNo++, in.itemId(), in.description(), in.quantity(),
                    in.unitOfMeasure(), new Money(in.unitPrice(), currency)));
        }
        return lines;
    }

    private static Currency parseCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new DomainException(RequisitionError.INVALID_REQUISITION, "Unknown currency code: " + code);
        }
    }

    private static int yearOf(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC).getYear();
    }

    private DomainException notFound() {
        return new DomainException(RequisitionError.REQUISITION_NOT_FOUND, "Requisition not found");
    }
}