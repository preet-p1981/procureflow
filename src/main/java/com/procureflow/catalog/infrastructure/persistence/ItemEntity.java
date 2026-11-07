package com.procureflow.catalog.infrastructure.persistence;

import com.procureflow.catalog.application.ItemDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "item")
public class ItemEntity {

    @Id
    private UUID id;

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "standard_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal standardPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ItemEntity() {
    }

    ItemEntity(UUID id, String sku, ItemDetails d) {
        this.id = id;
        this.sku = sku;
        this.active = true;
        apply(d);
    }

    void apply(ItemDetails d) {
        this.name = d.name();
        this.categoryId = d.categoryId();
        this.unitOfMeasure = d.unitOfMeasure();
        this.standardPrice = d.price();
        this.currency = d.currency();
    }

    void setActive(boolean active) {
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    UUID getId() { return id; }
    String getSku() { return sku; }
    String getName() { return name; }
    UUID getCategoryId() { return categoryId; }
    String getUnitOfMeasure() { return unitOfMeasure; }
    BigDecimal getStandardPrice() { return standardPrice; }
    String getCurrency() { return currency; }
    boolean isActive() { return active; }
}