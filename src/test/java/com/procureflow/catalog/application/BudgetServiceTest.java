package com.procureflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procureflow.catalog.application.port.BudgetStore;
import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.domain.Budget;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetServiceTest {

    private BudgetStore budgets;
    private DepartmentStore departments;
    private BudgetService service;
    private final UUID deptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        budgets = mock(BudgetStore.class);
        departments = mock(DepartmentStore.class);
        service = new BudgetService(budgets, departments);
        when(budgets.insert(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void assertCode(Runnable action, Object code) {
        assertThatThrownBy(action::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode()).isEqualTo(code);
    }

    @Test
    void createsBudgetForExistingDepartment() {
        when(departments.findById(deptId)).thenReturn(Optional.of(mock(DepartmentView.class)));

        BudgetView view = service.create(deptId, 2026, "INR", new BigDecimal("50000"));

        assertThat(view.departmentId()).isEqualTo(deptId);
        assertThat(view.allocated()).isEqualByComparingTo("50000");
        assertThat(view.available()).isEqualByComparingTo("50000");
        assertThat(view.currency()).isEqualTo("INR");
    }

    @Test
    void rejectsUnknownDepartment() {
        when(departments.findById(deptId)).thenReturn(Optional.empty());
        assertCode(() -> service.create(deptId, 2026, "INR", BigDecimal.TEN),
                CatalogError.DEPARTMENT_NOT_FOUND);
        verify(budgets, never()).insert(any());
    }

    @Test
    void rejectsUnknownCurrencyCode() {
        assertCode(() -> service.create(deptId, 2026, "XYZ", BigDecimal.TEN), CatalogError.INVALID_BUDGET);
    }

    @Test
    void rejectsNegativeAllocation() {
        when(departments.findById(deptId)).thenReturn(Optional.of(mock(DepartmentView.class)));
        assertCode(() -> service.create(deptId, 2026, "INR", new BigDecimal("-1")), CatalogError.INVALID_BUDGET);
    }

    @Test
    void getReportsMissingBudget() {
        UUID id = UUID.randomUUID();
        when(budgets.findById(id)).thenReturn(Optional.empty());
        assertCode(() -> service.get(id), CatalogError.BUDGET_NOT_FOUND);
    }

    @Test
    void findReturnsBudgetForDepartmentAndYear() {
        Budget b = Budget.create(deptId, 2026, com.procureflow.shared.domain.Money.of("100", "INR"));
        when(budgets.find(deptId, 2026)).thenReturn(Optional.of(b));
        assertThat(service.find(deptId, 2026).id()).isEqualTo(b.id());
    }
}