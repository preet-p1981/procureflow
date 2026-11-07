package com.procureflow.requisition.domain.port;

public interface RequisitionNumberGenerator {
    /** Returns the next number, for example PR-2026-000042. */
    String next(int year);
}