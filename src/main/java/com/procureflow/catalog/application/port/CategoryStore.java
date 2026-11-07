package com.procureflow.catalog.application.port;

import com.procureflow.catalog.application.CategoryView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryStore {
    boolean existsByCode(String code);
    CategoryView insert(String code,String name);
    Optional<CategoryView> findById(UUID id);
    List<CategoryView> findAll();
    CategoryView update(UUID id,String name,boolean active);
}
