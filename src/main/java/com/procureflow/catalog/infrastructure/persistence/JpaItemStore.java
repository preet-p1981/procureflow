package com.procureflow.catalog.infrastructure.persistence;

import com.procureflow.catalog.application.ItemDetails;
import com.procureflow.catalog.application.ItemView;
import com.procureflow.catalog.application.port.ItemStore;
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
class JpaItemStore implements ItemStore {

    private final SpringDataItemRepository repo;

    JpaItemStore(SpringDataItemRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySku(String sku) {
        return repo.existsBySku(sku);
    }

    @Override
    public ItemView insert(String sku, ItemDetails details) {
        try {
            return toView(repo.saveAndFlush(new ItemEntity(UUID.randomUUID(), sku, details)));
        } catch (DataIntegrityViolationException ex) {
            // the service already checked the category, so this is a SKU race: the unique constraint decides
            throw new DomainException(CatalogError.ITEM_SKU_EXISTS, "Item SKU already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemView> findById(UUID id) {
        return repo.findById(id).map(JpaItemStore::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemView> findAll(UUID categoryIdOrNull) {
        List<ItemEntity> rows = categoryIdOrNull == null
                ? repo.findAllByOrderBySkuAsc()
                : repo.findByCategoryIdOrderBySkuAsc(categoryIdOrNull);
        return rows.stream().map(JpaItemStore::toView).toList();
    }

    @Override
    public ItemView update(UUID id, ItemDetails details, boolean active) {
        ItemEntity entity = repo.findById(id).orElseThrow(() ->
                new DomainException(CatalogError.ITEM_NOT_FOUND, "Item not found"));
        entity.apply(details);
        entity.setActive(active);
        return toView(repo.saveAndFlush(entity));
    }

    private static ItemView toView(ItemEntity e) {
        return new ItemView(e.getId(), e.getSku(), e.getName(), e.getCategoryId(), e.getUnitOfMeasure(),
                e.getStandardPrice(), e.getCurrency(), e.isActive());
    }
}