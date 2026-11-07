package com.procureflow.approval.domain;

//bigger amouts needs ore senior approvers amounts in another currency are treated as above the limit //

import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import org.springframework.format.number.money.MonetaryAmountFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AmountThresholdRule implements ApprovalRule {
    public static final String FINANCE_ROLE = "FINANCE_APPROVER";
    public static final String EXECUTIVE_ROLE = "ADMIN";

    private final Money financeThreshold;
    private final Money executiveThreshold;

    public AmountThresholdRule(Money financeThreshold ,Money executiveThreshold){
        this.financeThreshold = Objects.requireNonNull(financeThreshold);
        this.executiveThreshold = Objects.requireNonNull(executiveThreshold);
        if(!financeThreshold.currency().equals(executiveThreshold.currency())){
            throw new DomainException(ApprovalError.INVALID_APPROVAL,"Threshold must use the same currency");
        }
        if (!executiveThreshold.isGreaterThan(financeThreshold)){
            throw new DomainException(ApprovalError.INVALID_APPROVAL,
                    "the executive threshold must be higher than the finance threshold");

        }
    }

    @Override
    public boolean appliesTo(ApprovalContext context){
        return exceeds(context.total(),financeThreshold);
    }

    @Override
    public List<ApprovalStepSpec> steps(ApprovalContext context){
        List<ApprovalStepSpec> steps = new ArrayList<>();
        steps.add(new ApprovalStepSpec(FINANCE_ROLE,48));
        if(exceeds(context.total(),executiveThreshold)){
            steps.add(new ApprovalStepSpec(EXECUTIVE_ROLE,72));
        }
        return steps;
    }

    @Override
    public int order(){
        return 20;
    }
    private static boolean exceeds(Money total, Money threshold){
        if(!total.currency().equals(threshold.currency())) {
            return true;
        }
        return total.isGreaterThan(threshold); //exact at limit;
    }

}
