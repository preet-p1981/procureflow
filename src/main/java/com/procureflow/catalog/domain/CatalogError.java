package com.procureflow.catalog.domain;

import com.procureflow.shared.domain.ErrorCode;
import com.procureflow.shared.domain.ErrorType;

public enum CatalogError implements ErrorCode {
    DEPARTMENT_NOT_FOUND(ErrorType.NOT_FOUND),
    DEPARTMENT_CODE_EXISTS(ErrorType.CONFLICT),
    BUDGET_NOT_FOUND(ErrorType.NOT_FOUND),
    BUDGET_ALREADY_EXISTS(ErrorType.CONFLICT),
    INVALID_BUDGET(ErrorType.VALIDATION),
    BUDGET_EXCEEDED(ErrorType.BUSINESS_RULE),
    CATEGORY_NOT_FOUND(ErrorType.NOT_FOUND),
    CATEGORY_CODE_EXISTS(ErrorType.CONFLICT);

    private final ErrorType type;

    CatalogError(ErrorType type){
        this.type = type;
    }

    @Override
    public String code(){
        return name();
    }

    @Override
    public ErrorType type(){
        return type;
    }
}
