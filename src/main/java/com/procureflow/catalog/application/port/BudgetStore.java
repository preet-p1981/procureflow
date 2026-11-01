package com.procureflow.catalog.application.port;

import com.procureflow.catalog.domain.Budget;
import com.procureflow.shared.domain.Money;

import java.util.Optional;
import java.util.UUID;

public interface BudgetStore {
    Budget insert(Budget budget);
    Optional<Budget> findById(UUID id);
    Optional<Budget> find(UUID departmentId,int fiscalYear);

    //Atomically reserve money ; throws budgetexceed if not available
    void commit(UUID budgetId, Money amount);


    /** Atomically gives reserved money back. */
    void release(UUID budgetId, Money amount);

    /** Atomically turns reserved money into spent money. */
    void consume(UUID budgetId, Money amount);
}
