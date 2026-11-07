package com.procureflow.requisition.domain;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

public record RequisitionLine(int lineNo, UUID itemId, String description, BigDecimal quantity,
                              String unitOfMeasure, Money unitPrice) {

    public RequisitionLine {
        if (lineNo < 1) {
            throw invalid("Line number must be at least 1");
        }
        if (description == null || description.isBlank() || description.trim().length() > 200) {
            throw invalid("Line " + lineNo + ": description is required (max 200 characters)");
        }
        description = description.trim();
        if (quantity == null || quantity.signum() <= 0) {
            throw invalid("Line " + lineNo + ": quantity must be greater than zero");
        }
        quantity = quantity.setScale(Money.SCALE, RoundingMode.HALF_EVEN);
        if (unitOfMeasure == null || unitOfMeasure.isBlank() || unitOfMeasure.trim().length() > 20) {
            throw invalid("Line " + lineNo + ": unit of measure is required (max 20 characters)");
        }
        unitOfMeasure = unitOfMeasure.trim().toUpperCase(Locale.ROOT);
        if (unitPrice == null || unitPrice.isNegative()) {
            throw invalid("Line " + lineNo + ": unit price must not be negative");
        }
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }

    private static DomainException invalid(String message) {
        return new DomainException(RequisitionError.INVALID_REQUISITION, message);
    }
}