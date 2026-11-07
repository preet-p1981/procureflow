package com.procureflow.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "category")
public class CategoryEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CategoryEntity() {
    }

    CategoryEntity(UUID id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = true;
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
    String getCode() { return code; }
    String getName() { return name; }
    boolean isActive() { return active; }
    void changeDetails(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
}