package com.procureflow.catalog.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateItemRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 20) String unitOfMeasure,
        @NotNull @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal standardPrice,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code") String currency,
        boolean active) {
}