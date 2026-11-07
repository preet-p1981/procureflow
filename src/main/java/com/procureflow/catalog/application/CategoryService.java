package com.procureflow.catalog.application;

import com.procureflow.catalog.application.port.CategoryStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryStore store;

    public CategoryService(CategoryStore store) {
        this.store = store;
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public CategoryView create(String code, String name) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (store.existsByCode(normalized)) {
            throw new DomainException(CatalogError.CATEGORY_CODE_EXISTS, "Category code already exists");
        }
        return store.insert(normalized, name.trim());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    public CategoryView update(UUID id, String name, boolean active) {
        return store.update(id, name.trim(), active);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public CategoryView get(UUID id) {
        return store.findById(id).orElseThrow(() ->
                new DomainException(CatalogError.CATEGORY_NOT_FOUND, "Category not found"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CATALOG_READ')")
    public List<CategoryView> list() {
        return store.findAll();
    }
}