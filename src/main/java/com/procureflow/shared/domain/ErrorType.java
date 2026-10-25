package com.procureflow.shared.domain;

public enum ErrorType {
    VALIDATION,      // bad input
    NOT_FOUND,       // resource does not exist
    CONFLICT,        // version conflict, duplicate
    BUSINESS_RULE    // action not allowed in current state
}