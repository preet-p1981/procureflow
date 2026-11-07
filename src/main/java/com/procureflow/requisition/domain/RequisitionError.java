package com.procureflow.requisition.domain;

import com.procureflow.shared.domain.ErrorCode;
import com.procureflow.shared.domain.ErrorType;

public enum RequisitionError implements ErrorCode {

    REQUISITION_NOT_FOUND(ErrorType.NOT_FOUND),
    INVALID_REQUISITION(ErrorType.VALIDATION),
    INVALID_REQUISITION_STATE(ErrorType.BUSINESS_RULE);

    private final ErrorType type;

    RequisitionError(ErrorType type) {
        this.type = type;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorType type() {
        return type;
    }
}