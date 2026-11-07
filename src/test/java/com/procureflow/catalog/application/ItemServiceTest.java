package com.procureflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procureflow.catalog.application.port.CategoryStore;
import com.procureflow.catalog.application.port.ItemStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemServiceTest {

    private ItemStore items;
    private CategoryStore categories;
    private ItemService service;
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        items = mock(ItemStore.class);
        categories = mock(CategoryStore.class);
        service = new ItemService(items, categories);
        when(items.insert(any(), any())).thenAnswer(inv -> {
            ItemDetails d = inv.getArgument(1);
            return new ItemView(UUID.randomUUID(), inv.getArgument(0), d.name(), d.categoryId(),
                    d.unitOfMeasure(), d.price(), d.currency(), true);
        });
    }

    private ItemDetails details(String price, String currency) {
        return new ItemDetails(" Ballpoint pen ", categoryId, "box", new BigDecimal(price), currency);
    }

    private void categoryIs(boolean active) {
        when(categories.findById(categoryId))
                .thenReturn(Optional.of(new CategoryView(categoryId, "OFFICE", "Office", active)));
    }

    private void assertCode(Runnable action, Object code) {
        assertThatThrownBy(action::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode()).isEqualTo(code);
    }

    @Test
    void createsItemAndNormalizesFields() {
        categoryIs(true);
        ItemView view = service.create("  pen-01 ", details("12.5", "INR"));
        assertThat(view.sku()).isEqualTo("PEN-01");
        assertThat(view.name()).isEqualTo("Ballpoint pen");
        assertThat(view.unitOfMeasure()).isEqualTo("BOX");
        assertThat(view.standardPrice()).isEqualByComparingTo("12.5");
    }

    @Test
    void rejectsDuplicateSku() {
        when(items.existsBySku("PEN-01")).thenReturn(true);
        assertCode(() -> service.create("pen-01", details("1", "INR")), CatalogError.ITEM_SKU_EXISTS);
        verify(items, never()).insert(any(), any());
    }

    @Test
    void rejectsUnknownCategory() {
        when(categories.findById(categoryId)).thenReturn(Optional.empty());
        assertCode(() -> service.create("pen-01", details("1", "INR")), CatalogError.CATEGORY_NOT_FOUND);
    }

    @Test
    void rejectsInactiveCategory() {
        categoryIs(false);
        assertCode(() -> service.create("pen-01", details("1", "INR")), CatalogError.CATEGORY_INACTIVE);
    }

    @Test
    void rejectsNegativePriceAndUnknownCurrency() {
        categoryIs(true);
        assertCode(() -> service.create("a", details("-1", "INR")), CatalogError.INVALID_ITEM);
        assertCode(() -> service.create("b", details("1", "XYZ")), CatalogError.INVALID_ITEM);
    }

    @Test
    void updateKeepingSameCategoryDoesNotRecheckCategory() {
        UUID id = UUID.randomUUID();
        when(items.findById(id)).thenReturn(Optional.of(
                new ItemView(id, "PEN-01", "Pen", categoryId, "BOX", BigDecimal.ONE, "INR", true)));
        service.update(id, details("2", "INR"), true);
        verify(categories, never()).findById(any());
        verify(items).update(any(), any(), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void updateMovingToInactiveCategoryIsRejected() {
        UUID id = UUID.randomUUID();
        UUID oldCategory = UUID.randomUUID();
        when(items.findById(id)).thenReturn(Optional.of(
                new ItemView(id, "PEN-01", "Pen", oldCategory, "BOX", BigDecimal.ONE, "INR", true)));
        categoryIs(false);
        assertCode(() -> service.update(id, details("2", "INR"), true), CatalogError.CATEGORY_INACTIVE);
    }

    @Test
    void updateReportsMissingItem() {
        UUID id = UUID.randomUUID();
        when(items.findById(id)).thenReturn(Optional.empty());
        assertCode(() -> service.update(id, details("2", "INR"), true), CatalogError.ITEM_NOT_FOUND);
    }
}