package com.procureflow.approval.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApprovalPolicy {

    private final List<ApprovalRule> rules;

    public ApprovalPolicy(List<ApprovalRule> rules) {
        this.rules = rules.stream().sorted(Comparator.comparingInt(ApprovalRule::order)).toList();
    }

    public List<ApprovalStepSpec> buildChain(ApprovalContext context) {
        Map<String, ApprovalStepSpec> byRole = new LinkedHashMap<>();   // keeps order, drops duplicate roles
        for (ApprovalRule rule : rules) {
            if (rule.appliesTo(context)) {
                for (ApprovalStepSpec spec : rule.steps(context)) {
                    byRole.putIfAbsent(spec.approverRole(), spec);
                }
            }
        }
        if (byRole.isEmpty()) {
            // every request needs at least one approver
            byRole.put(AmountThresholdRule.FINANCE_ROLE, new ApprovalStepSpec(AmountThresholdRule.FINANCE_ROLE, 48));
        }
        return List.copyOf(byRole.values());
    }
}