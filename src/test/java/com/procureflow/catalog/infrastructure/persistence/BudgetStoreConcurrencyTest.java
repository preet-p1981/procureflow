package com.procureflow.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.catalog.application.DepartmentView;
import com.procureflow.catalog.application.port.BudgetStore;
import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.domain.Budget;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BudgetStoreConcurrencyTest {

    @Autowired BudgetStore budgets;
    @Autowired DepartmentStore departments;
    @Autowired JdbcTemplate jdbc;

    private UUID departmentId;
    private UUID budgetId;

    private static Money inr(String amount) {
        return Money.of(amount, "INR");
    }

    @BeforeEach
    void setUp() {
        String code = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DepartmentView dept = departments.insert(code, "Budget test department");
        departmentId = UUID.fromString(dept.id());                     // adjust if your DepartmentView names it differently
        budgetId = budgets.insert(Budget.create(departmentId, 2026, inr("1000"))).id();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM budget WHERE department_id = ?", departmentId);
        jdbc.update("DELETE FROM department WHERE id = ?", departmentId);
    }

    @Test
    void parallelCommitsNeverOverspend() throws Exception {
        int attempts = 50;                              // 50 x 30 = 1500 requested, only 1000 exists
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            Callable<Boolean> task = () -> {
                startGate.await();
                try {
                    budgets.commit(budgetId, inr("30"));
                    return true;
                } catch (DomainException ex) {
                    assertThat(ex.errorCode()).isEqualTo(CatalogError.BUDGET_EXCEEDED);
                    return false;
                }
            };
            results.add(pool.submit(task));
        }
        startGate.countDown();                          // release all threads at once

        long successes = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) successes++;
        }
        pool.shutdown();

        Budget after = budgets.findById(budgetId).orElseThrow();
        assertThat(successes).isEqualTo(33);            // 33 x 30 = 990, the 34th would exceed 1000
        assertThat(after.committed()).isEqualTo(inr("990"));
        assertThat(after.available()).isEqualTo(inr("10"));
    }

    @Test
    void releaseAndConsumeMoveTheNumbers() {
        budgets.commit(budgetId, inr("500"));
        budgets.consume(budgetId, inr("200"));
        budgets.release(budgetId, inr("100"));

        Budget after = budgets.findById(budgetId).orElseThrow();
        assertThat(after.committed()).isEqualTo(inr("200"));
        assertThat(after.consumed()).isEqualTo(inr("200"));
        assertThat(after.available()).isEqualTo(inr("600"));
    }

    @Test
    void cannotReleaseOrConsumeMoreThanCommitted() {
        budgets.commit(budgetId, inr("100"));
        assertThatThrownBy(() -> budgets.release(budgetId, inr("101")))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CatalogError.INVALID_BUDGET);
        assertThatThrownBy(() -> budgets.consume(budgetId, inr("101")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void reportsMissingBudgetAndWrongCurrency() {
        assertThatThrownBy(() -> budgets.commit(UUID.randomUUID(), inr("10")))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CatalogError.BUDGET_NOT_FOUND);

        assertThatThrownBy(() -> budgets.commit(budgetId, Money.of("10", "USD")))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CommonError.CURRENCY_MISMATCH);
    }

    @Test
    void secondBudgetForSameDepartmentAndYearIsRejected() {
        assertThatThrownBy(() -> budgets.insert(Budget.create(departmentId, 2026, inr("500"))))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CatalogError.BUDGET_ALREADY_EXISTS);
    }
}