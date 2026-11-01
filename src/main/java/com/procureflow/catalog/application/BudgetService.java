package com.procureflow.catalog.application;

import com.procureflow.catalog.application.port.BudgetStore;
import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.catalog.domain.Budget;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetStore budgets;
    private final DepartmentStore departments;

    public BudgetService (BudgetStore budgets,DepartmentStore departments){
        this.budgets = budgets;
        this.departments = departments;
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public BudgetView create(UUID departmentId, int fiscalYear, String currencyCode, BigDecimal allocated){
        Currency currency = parseCurrency(currencyCode);
        departments.findById(departmentId).orElseThrow(()->
                new DomainException(CatalogError.DEPARTMENT_NOT_FOUND,"Department not found"));
        Budget budget = Budget.create(departmentId,fiscalYear,new Money(allocated,currency));
        return BudgetView.from(budgets.insert(budget));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public BudgetView get(UUID id){
        return budgets.findById(id).map(BudgetView::from).orElseThrow(this::notFound);

    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthoriy('CATALOG_READ')")
    public BudgetView find(UUID departmentId,int fiscalYear){
        return budgets.find(departmentId,fiscalYear).map(BudgetView::from).orElseThrow(this::notFound);
    }
    private DomainException notFound(){
        return new DomainException(CatalogError.BUDGET_NOT_FOUND,"budget not found");

    }
    private static Currency parseCurrency(String code){
        try{
            return Currency.getInstance(code);
        }catch(IllegalArgumentException | NullPointerException ex){
            throw new DomainException(CatalogError.INVALID_BUDGET,"Unknown currency");
        }

    }

}
