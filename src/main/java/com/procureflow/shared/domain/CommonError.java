package com.procureflow.shared.domain;

public enum CommonError implements ErrorCode {

    CURRENCY_MISMATCH(ErrorType.BUSINESS_RULE),
    VALIDATION_FAILED(ErrorType.VALIDATION),
    RESOURCE_NOT_FOUND(ErrorType.NOT_FOUND),
    CONFLICT(ErrorType.CONFLICT);

    private final ErrorType type;

    CommonError(ErrorType type) {
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