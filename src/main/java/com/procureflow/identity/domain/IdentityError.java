package com.procureflow.identity.domain;

import com.procureflow.shared.domain.ErrorCode;
import com.procureflow.shared.domain.ErrorType;

public enum IdentityError implements ErrorCode {

    INVALID_EMAIL(ErrorType.VALIDATION),
    WEAK_PASSWORD(ErrorType.VALIDATION),
    EMAIL_ALREADY_REGISTERED(ErrorType.CONFLICT),
    USER_NOT_FOUND(ErrorType.NOT_FOUND),
    INVALID_CREDENTIALS(ErrorType.UNAUTHENTICATED),
    ACCOUNT_LOCKED(ErrorType.FORBIDDEN),
    ACCOUNT_DISABLED(ErrorType.FORBIDDEN);

    private final ErrorType type;

    IdentityError(ErrorType type) {
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