package com.procureflow.requisition.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record RequisitionLineRequest(
        UUID itemId,
        @NotBlank @Size(max = 200) String description,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal quantity,
        @NotBlank @Size(max = 20) String unitOfMeasure,
        @NotNull @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal unitPrice) {
}