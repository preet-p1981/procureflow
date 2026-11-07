package com.procureflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procureflow.catalog.application.port.CategoryStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

    private CategoryStore store;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        store = mock(CategoryStore.class);
        service = new CategoryService(store);
    }

    @Test
    void normalizesCodeAndTrimsName() {
        when(store.existsByCode("OFFICE")).thenReturn(false);
        CategoryView stored = new CategoryView(UUID.randomUUID(), "OFFICE", "Office supplies", true);
        when(store.insert("OFFICE", "Office supplies")).thenReturn(stored);

        assertThat(service.create("  office ", " Office supplies ")).isEqualTo(stored);
    }

    @Test
    void rejectsDuplicateCode() {
        when(store.existsByCode("OFFICE")).thenReturn(true);
        assertThatThrownBy(() -> service.create("office", "Office"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CatalogError.CATEGORY_CODE_EXISTS);
        verify(store, never()).insert("OFFICE", "Office");
    }

    @Test
    void getReportsMissingCategory() {
        UUID id = UUID.randomUUID();
        when(store.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CatalogError.CATEGORY_NOT_FOUND);
    }
}