package com.procureflow.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDetails(String name, UUID categoryId, String unitOfMeasure,
                          BigDecimal price, String currency) {
}