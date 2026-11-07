package com.procureflow.approval.domain;

import java.util.List;

//every req needs department manager unless the requester is a manager //
public class DepartmentManagerRule implements  ApprovalRule{

    public static final String ROLE = "DEPARTMENT_MANAGER";
    private static final int SLA_HOURS = 24;

    @Override
    public boolean appliesTo(ApprovalContext context){
        return !context.requesterRoles().contains(ROLE);
    }

    @Override
    public List<ApprovalStepSpec> steps(ApprovalContext context){
        return List.of(new ApprovalStepSpec(ROLE,SLA_HOURS));
    }

    @Override
    public int order(){
        return 10;
    }

}
