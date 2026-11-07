package com.procureflow.catalog.application.port;

import com.procureflow.catalog.application.ItemDetails;
import com.procureflow.catalog.application.ItemView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemStore {
    boolean existsBySku(String sku);
    ItemView insert(String sku, ItemDetails details);
    Optional<ItemView> findById(UUID id);
    List<ItemView> findAll(UUID categoryIdOrNull);
    ItemView update(UUID id, ItemDetails details, boolean active);
}