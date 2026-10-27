package com.procureflow.identity.domain;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;

import java.time.Duration;

public record LockoutPolicy(int maxFailedAttempts , Duration lockDuration) {

    public LockoutPolicy{
        if(maxFailedAttempts < 1){
            throw new DomainException(CommonError.VALIDATION_FAILED,"max attempt must be atleast one ");
        }
        if(lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()){
            throw new DomainException(CommonError.VALIDATION_FAILED,"lockduration must be positive");
        }
    }

    public static LockoutPolicy standard(){
        return new LockoutPolicy(5,Duration.ofMinutes(15));
    }
}
