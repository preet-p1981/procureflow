package com.procureflow.approval.domain;

import com.procureflow.shared.domain.ErrorCode;
import com.procureflow.shared.domain.ErrorType;

public enum ApprovalError implements ErrorCode {

    APPROVAL_NOT_FOUND(ErrorType.NOT_FOUND),
    INVALID_APPROVAL(ErrorType.VALIDATION),
    INVALID_APPROVAL_STATE(ErrorType.BUSINESS_RULE),
    NOT_AUTHORIZED_APPROVER(ErrorType.FORBIDDEN),
    SELF_APPROVAL_NOT_ALLOWED(ErrorType.FORBIDDEN),
    APPROVER_ALREADY_SIGNED(ErrorType.FORBIDDEN);

    private final ErrorType type;

    ApprovalError(ErrorType type) {
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