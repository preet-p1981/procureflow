package com.procureflow.catalog.application;

import com.procureflow.catalog.domain.Budget;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetView (UUID id, UUID departmentId , int fiscalYear, String currency, BigDecimal allocated, BigDecimal commited, BigDecimal consumed, BigDecimal available){

    public static BudgetView from(Budget b){
        return new BudgetView(b.id(),b.departmentId(),b.fiscalYear(),b.allocated().currency().getCurrencyCode(),b.allocated().amount(),b.committed().amount(),b.consumed().amount(),b.available().amount());

    }
}
