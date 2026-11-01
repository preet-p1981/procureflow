package com.procureflow.catalog.infrastructure.persistence;

import com.procureflow.catalog.application.port.BudgetStore;
import com.procureflow.catalog.domain.Budget;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.ErrorCode;
import com.procureflow.shared.domain.Money;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
class JpaBudgetStore implements BudgetStore {

    private final SpringDataBudgetRepository repo;

    JpaBudgetStore(SpringDataBudgetRepository repo) {
        this.repo = repo;
    }

    @Override
    public Budget insert(Budget b) {
        BudgetEntity entity = new BudgetEntity(b.id(), b.departmentId(), b.fiscalYear(),
                b.allocated().currency().getCurrencyCode(),
                b.allocated().amount(), b.committed().amount(), b.consumed().amount());
        try {
            return toDomain(repo.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(CatalogError.BUDGET_ALREADY_EXISTS,
                    "A budget already exists for this department and fiscal year");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Budget> findById(UUID id) {
        return repo.findById(id).map(JpaBudgetStore::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Budget> find(UUID departmentId, int fiscalYear) {
        return repo.findByDepartmentIdAndFiscalYear(departmentId, fiscalYear)
                .map(JpaBudgetStore::toDomain);
    }

    @Override
    public void commit(UUID id, Money amount) {
        requirePositive(amount);
        int rows = repo.atomicCommit(id, amount.amount(), code(amount));
        if (rows == 0) {
            throw explainFailure(id, amount, CatalogError.BUDGET_EXCEEDED, "Budget exceeded");
        }
    }

    @Override
    public void release(UUID id, Money amount) {
        requirePositive(amount);
        int rows = repo.atomicRelease(id, amount.amount(), code(amount));
        if (rows == 0) {
            throw explainFailure(id, amount, CatalogError.INVALID_BUDGET,
                    "Cannot release more than the committed amount");
        }
    }

    @Override
    public void consume(UUID id, Money amount) {
        requirePositive(amount);
        int rows = repo.atomicConsume(id, amount.amount(), code(amount));
        if (rows == 0) {
            throw explainFailure(id, amount, CatalogError.INVALID_BUDGET,
                    "Cannot consume more than the committed amount");
        }
    }

    /** The update changed nothing: work out whether the budget is missing, in another currency, or just too small. */
    private DomainException explainFailure(UUID id, Money amount, ErrorCode fallback, String message) {
        BudgetEntity budget = repo.findById(id).orElse(null);
        if (budget == null) {
            return new DomainException(CatalogError.BUDGET_NOT_FOUND, "Budget not found");
        }
        if (!budget.getCurrency().equals(code(amount))) {
            return new DomainException(CommonError.CURRENCY_MISMATCH,
                    "Currency mismatch: " + budget.getCurrency() + " vs " + code(amount));
        }
        return new DomainException(fallback, message);
    }

    private static void requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new DomainException(CatalogError.INVALID_BUDGET, "Amount must be greater than zero");
        }
    }

    private static String code(Money amount) {
        return amount.currency().getCurrencyCode();
    }

    private static Budget toDomain(BudgetEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());
        return Budget.reconstitute(e.getId(), e.getDepartmentId(), e.getFiscalYear(),
                new Money(e.getAllocated(), currency),
                new Money(e.getCommitted(), currency),
                new Money(e.getConsumed(), currency),
                e.getVersion() == null ? 0L : e.getVersion());
    }
}