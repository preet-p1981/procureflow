package com.procureflow.requisition.application;

import java.math.BigDecimal;
import java.util.UUID;

/** One line as typed by the requester. Line numbers are assigned by position, not by the client. */
public record RequisitionLineInput(UUID itemId, String description, BigDecimal quantity,
                                   String unitOfMeasure, BigDecimal unitPrice) {
}