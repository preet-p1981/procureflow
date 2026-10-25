package com.procureflow.shared.domain;

import java.util.Objects;

public class DomainException extends  RuntimeException{

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode , String message){
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
    }
    public ErrorCode errorCode(){
        return errorCode;
    }
}
