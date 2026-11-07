package com.procureflow.approval.domain;

import java.util.List;

public interface ApprovalRule {
    boolean appliesTo(ApprovalContext context);
    List<ApprovalStepSpec> steps(ApprovalContext context);

    // lower numbers run first so the chian order is predictable //
    int order();
}
