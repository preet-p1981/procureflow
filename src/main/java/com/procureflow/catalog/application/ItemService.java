package com.procureflow.catalog.application;

import com.procureflow.catalog.application.port.CategoryStore;
import com.procureflow.catalog.application.port.ItemStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private final ItemStore items;
    private final CategoryStore categories;

    public ItemService(ItemStore items, CategoryStore categories) {
        this.items = items;
        this.categories = categories;
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public ItemView create(String sku, ItemDetails details) {
        String normalizedSku = sku.trim().toUpperCase(Locale.ROOT);
        if (items.existsBySku(normalizedSku)) {
            throw new DomainException(CatalogError.ITEM_SKU_EXISTS, "Item SKU already exists");
        }
        requireActiveCategory(details.categoryId());
        return items.insert(normalizedSku, normalize(details));
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public ItemView update(UUID id, ItemDetails details, boolean active) {
        ItemView current = items.findById(id).orElseThrow(this::notFound);
        if (!current.categoryId().equals(details.categoryId())) {
            requireActiveCategory(details.categoryId());   // only re-checked when the category changes
        }
        return items.update(id, normalize(details), active);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public ItemView get(UUID id) {
        return items.findById(id).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public List<ItemView> list(UUID categoryIdOrNull) {
        return items.findAll(categoryIdOrNull);
    }

    private void requireActiveCategory(UUID categoryId) {
        CategoryView category = categories.findById(categoryId).orElseThrow(() ->
                new DomainException(CatalogError.CATEGORY_NOT_FOUND, "Category not found"));
        if (!category.active()) {
            throw new DomainException(CatalogError.CATEGORY_INACTIVE, "Category is not active");
        }
    }

    /** Trims text, validates currency and price, and fixes the price to 4 decimal places. */
    private ItemDetails normalize(ItemDetails d) {
        Currency currency;
        try {
            currency = Currency.getInstance(d.currency());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new DomainException(CatalogError.INVALID_ITEM, "Unknown currency code: " + d.currency());
        }
        Money price = new Money(d.price(), currency);
        if (price.isNegative()) {
            throw new DomainException(CatalogError.INVALID_ITEM, "Price must not be negative");
        }
        return new ItemDetails(d.name().trim(), d.categoryId(), d.unitOfMeasure().trim().toUpperCase(Locale.ROOT),
                price.amount(), currency.getCurrencyCode());
    }

    private DomainException notFound() {
        return new DomainException(CatalogError.ITEM_NOT_FOUND, "Item not found");
    }
}