package com.procureflow.approval.domain;

import com.procureflow.shared.domain.DomainException;

public record ApprovalStepSpec(String approverRole, int slaHours) {

    public ApprovalStepSpec {
        if (approverRole == null || approverRole.isBlank()) {
            throw new DomainException(ApprovalError.INVALID_APPROVAL, "Approver role is required");
        }
        approverRole = approverRole.trim().toUpperCase();
        if (slaHours < 1) {
            throw new DomainException(ApprovalError.INVALID_APPROVAL, "SLA must be at least 1 hour");
        }
    }
}