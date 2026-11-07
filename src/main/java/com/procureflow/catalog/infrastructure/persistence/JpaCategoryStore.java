package com.procureflow.catalog.infrastructure.persistence;

import com.procureflow.catalog.application.CategoryView;
import com.procureflow.catalog.application.port.CategoryStore;
import com.procureflow.catalog.domain.CatalogError;
import com.procureflow.shared.domain.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
class JpaCategoryStore implements CategoryStore {

    private final SpringDataCategoryRepository repo;

    JpaCategoryStore(SpringDataCategoryRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public CategoryView insert(String code, String name) {
        try {
            return toView(repo.saveAndFlush(new CategoryEntity(UUID.randomUUID(), code, name)));
        } catch (DataIntegrityViolationException ex) {
            // two requests can pass the existsByCode check together; the unique constraint decides
            throw new DomainException(CatalogError.CATEGORY_CODE_EXISTS, "Category code already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryView> findById(UUID id) {
        return repo.findById(id).map(JpaCategoryStore::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryView> findAll() {
        return repo.findAllByOrderByCodeAsc().stream().map(JpaCategoryStore::toView).toList();
    }

    @Override
    public CategoryView update(UUID id, String name, boolean active) {
        CategoryEntity entity = repo.findById(id).orElseThrow(() ->
                new DomainException(CatalogError.CATEGORY_NOT_FOUND, "Category not found"));
        entity.changeDetails(name, active);
        return toView(repo.saveAndFlush(entity));
    }

    private static CategoryView toView(CategoryEntity e) {
        return new CategoryView(e.getId(), e.getCode(), e.getName(), e.isActive());
    }
}