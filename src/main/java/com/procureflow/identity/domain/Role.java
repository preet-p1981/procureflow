package com.procureflow.identity.domain;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;

import java.util.Arrays;
import java.util.Set;

public record Role (String code , Set<String> permission){

    public Role{
        if(code == null || code.isBlank()){
            throw new DomainException(CommonError.VALIDATION_FAILED,"Role cant be blank");
        }
        permission = Set.copyOf(permission == null ? Set.of() : permission);
    }

    public static Role of(String code ,String... permission){
        return new Role(code,Set.copyOf(Arrays.asList(permission)));
    }
}
