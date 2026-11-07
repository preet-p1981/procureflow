package com.procureflow.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemView(UUID id, String sku, String name, UUID categoryId, String unitOfMeasure,
                       BigDecimal standardPrice, String currency, boolean active) {
}