package com.procureflow.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId{
        Objects.requireNonNull(value,"user id must not be null");
    }

    public static UserId newId(){
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String value){
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString(){
        return value.toString();
    }
}
