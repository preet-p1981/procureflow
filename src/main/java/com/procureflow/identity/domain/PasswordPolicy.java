package com.procureflow.identity.domain;

import com.procureflow.shared.domain.DomainException;

public final class PasswordPolicy {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    private PasswordPolicy(){};

    public static void validate(String raw){
        if(raw == null || raw.length() < MIN_LENGTH || raw.length()>MAX_LENGTH ||raw.chars().noneMatch(Character::isUpperCase) || raw.chars().noneMatch(Character::isLowerCase) || raw.chars().noneMatch(Character::isDigit)){
            throw new DomainException(IdentityError.WEAK_PASSWORD,"Password must be 8-72 characters with upper case,lowercase and a digit");


        }
    }
}
