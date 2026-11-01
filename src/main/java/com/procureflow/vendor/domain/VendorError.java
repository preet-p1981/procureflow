package com.procureflow.vendor.domain;

import com.procureflow.shared.domain.ErrorCode;
import com.procureflow.shared.domain.ErrorType;

public enum VendorError implements ErrorCode {

    INVALID_TAX_ID(ErrorType.VALIDATION),
    VENDOR_NOT_FOUND(ErrorType.NOT_FOUND),
    TAX_ID_ALREADY_REGISTERED(ErrorType.CONFLICT),
    VENDOR_INVALID_TRANSITION(ErrorType.BUSINESS_RULE);

    private final ErrorType type;

    VendorError(ErrorType type) {
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