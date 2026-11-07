package com.procureflow.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    boolean existsByCode(String code);
    List<CategoryEntity> findAllByOrderByCodeAsc();
}
