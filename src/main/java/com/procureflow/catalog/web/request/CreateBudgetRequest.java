package com.procureflow.catalog.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateBudgetRequest(
        @NotNull UUID departmentId,
        @Min(2000) @Max(2100) int fiscalYear,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code") String currency,
        @NotNull @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal allocated) {
}