package com.procureflow.requisition.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procureflow.catalog.application.DepartmentView;
import com.procureflow.catalog.application.ItemView;
import com.procureflow.catalog.application.port.BudgetStore;
import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.application.port.ItemStore;
import com.procureflow.catalog.domain.Budget;
import com.procureflow.requisition.application.port.CurrentUser;
import com.procureflow.requisition.domain.PurchaseRequisition;
import com.procureflow.requisition.domain.RequisitionError;
import com.procureflow.requisition.domain.RequisitionLine;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.requisition.domain.port.RequisitionNumberGenerator;
import com.procureflow.requisition.domain.port.RequisitionRepository;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class RequisitionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private static final Currency INR = Currency.getInstance("INR");

    private RequisitionRepository requisitions;
    private RequisitionNumberGenerator numbers;
    private CurrentUser currentUser;
    private DepartmentStore departments;
    private ItemStore items;
    private BudgetStore budgets;
    private RequisitionService service;

    private final UUID me = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        requisitions = mock(RequisitionRepository.class);
        numbers = mock(RequisitionNumberGenerator.class);
        currentUser = mock(CurrentUser.class);
        departments = mock(DepartmentStore.class);
        items = mock(ItemStore.class);
        budgets = mock(BudgetStore.class);
        ObjectProvider<Clock> clockProvider = mock(ObjectProvider.class);
        when(clockProvider.getIfAvailable(any())).thenReturn(Clock.fixed(NOW, ZoneOffset.UTC));

        when(currentUser.id()).thenReturn(me);
        when(numbers.next(2026)).thenReturn("PR-2026-000001");
        when(departments.findById(departmentId)).thenReturn(Optional.of(mock(DepartmentView.class)));
        when(requisitions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new RequisitionService(requisitions, numbers, currentUser, departments, items, budgets,
                clockProvider);
    }

    private RequisitionLineInput input(UUID itemId, String qty, String price) {
        return new RequisitionLineInput(itemId, "Ballpoint pen", new BigDecimal(qty), "box", new BigDecimal(price));
    }

    private PurchaseRequisition storedDraft(UUID owner) {
        PurchaseRequisition r = PurchaseRequisition.createDraft("PR-2026-000009", owner, departmentId, INR,
                "Need pens", List.of(new RequisitionLine(1, null, "Pen", BigDecimal.TEN, "box", Money.of("5", "INR"))));
        when(requisitions.findById(r.id())).thenReturn(Optional.of(r));
        return r;
    }

    private void assertCode(Runnable action, RequisitionError code) {
        assertThatThrownBy(action::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode()).isEqualTo(code);
    }

    @Test
    void createDraftAssignsNumberOwnerAndLineNumbers() {
        RequisitionView view = service.createDraft(new CreateRequisitionCommand(departmentId, "INR", "Need pens",
                List.of(input(null, "10", "5"), input(null, "2", "100"))));

        assertThat(view.number()).isEqualTo("PR-2026-000001");
        assertThat(view.requesterId()).isEqualTo(me);
        assertThat(view.status()).isEqualTo(RequisitionStatus.DRAFT);
        assertThat(view.lines()).extracting(RequisitionView.LineView::lineNo).containsExactly(1, 2);
        assertThat(view.total()).isEqualByComparingTo("250");
    }

    @Test
    void createRejectsUnknownDepartmentCurrencyAndItems() {
        UUID other = UUID.randomUUID();
        when(departments.findById(other)).thenReturn(Optional.empty());
        assertCode(() -> service.createDraft(new CreateRequisitionCommand(other, "INR", "x", List.of())),
                RequisitionError.INVALID_REQUISITION);
        assertCode(() -> service.createDraft(new CreateRequisitionCommand(departmentId, "XYZ", "x", List.of())),
                RequisitionError.INVALID_REQUISITION);

        UUID missingItem = UUID.randomUUID();
        when(items.findById(missingItem)).thenReturn(Optional.empty());
        assertCode(() -> service.createDraft(new CreateRequisitionCommand(departmentId, "INR", "x",
                List.of(input(missingItem, "1", "1")))), RequisitionError.INVALID_REQUISITION);

        UUID inactiveItem = UUID.randomUUID();
        when(items.findById(inactiveItem)).thenReturn(Optional.of(
                new ItemView(inactiveItem, "OLD-1", "Old", UUID.randomUUID(), "BOX", BigDecimal.ONE, "INR", false)));
        assertCode(() -> service.createDraft(new CreateRequisitionCommand(departmentId, "INR", "x",
                List.of(input(inactiveItem, "1", "1")))), RequisitionError.INVALID_REQUISITION);
    }

    @Test
    void ownerCanUpdateAndSubmit() {
        PurchaseRequisition r = storedDraft(me);
        service.updateDraft(r.id(), "Updated reason", List.of(input(null, "3", "10")));
        RequisitionView view = service.submit(r.id());
        assertThat(view.status()).isEqualTo(RequisitionStatus.PENDING_APPROVAL);
        assertThat(view.total()).isEqualByComparingTo("30");
    }

    @Test
    void otherUsersCannotTouchSomeonesRequisition() {
        PurchaseRequisition r = storedDraft(UUID.randomUUID());
        assertCode(() -> service.submit(r.id()), RequisitionError.REQUISITION_NOT_FOUND);
        assertCode(() -> service.updateDraft(r.id(), "x", List.of()), RequisitionError.REQUISITION_NOT_FOUND);
        assertCode(() -> service.cancel(r.id()), RequisitionError.REQUISITION_NOT_FOUND);
    }

    @Test
    void cancellingDraftDoesNotTouchBudget() {
        PurchaseRequisition r = storedDraft(me);
        assertThat(service.cancel(r.id()).status()).isEqualTo(RequisitionStatus.CANCELLED);
        verify(budgets, never()).release(any(), any());
    }

    @Test
    void cancellingApprovedRequisitionReleasesItsBudget() {
        PurchaseRequisition r = storedDraft(me);
        r.submit(NOW);
        r.approve(Instant.parse("2026-10-30T09:00:00Z"));
        Budget budget = Budget.create(departmentId, 2026, Money.of("1000", "INR"));
        when(budgets.find(departmentId, 2026)).thenReturn(Optional.of(budget));

        service.cancel(r.id());

        verify(budgets).release(budget.id(), Money.of("50", "INR"));    // 10 x 5
    }

    @Test
    void getHidesOthersRequisitionsUnlessReadAll() {
        PurchaseRequisition r = storedDraft(UUID.randomUUID());
        when(currentUser.hasAuthority("REQ_READ_ALL")).thenReturn(false);
        assertCode(() -> service.get(r.id()), RequisitionError.REQUISITION_NOT_FOUND);

        when(currentUser.hasAuthority("REQ_READ_ALL")).thenReturn(true);
        assertThat(service.get(r.id()).id()).isEqualTo(r.id());
    }
}