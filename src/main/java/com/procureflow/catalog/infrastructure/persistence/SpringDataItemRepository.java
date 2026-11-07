package com.procureflow.catalog.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataItemRepository extends JpaRepository<ItemEntity, UUID> {

    boolean existsBySku(String sku);

    List<ItemEntity> findAllByOrderBySkuAsc();

    List<ItemEntity> findByCategoryIdOrderBySkuAsc(UUID categoryId);
}