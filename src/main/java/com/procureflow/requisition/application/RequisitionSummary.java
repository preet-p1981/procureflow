package com.procureflow.requisition.application;

import com.procureflow.requisition.domain.RequisitionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequisitionSummary(UUID id, String number, UUID requesterId, UUID departmentId,
                                 String currency, BigDecimal total, RequisitionStatus status,
                                 Instant submittedAt, Instant createdAt) {
}