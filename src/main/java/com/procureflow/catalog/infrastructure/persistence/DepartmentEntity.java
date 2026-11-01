package com.procureflow.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "department")
class DepartmentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;   // null while new, so Spring Data knows to INSERT

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DepartmentEntity() {}

    DepartmentEntity(UUID id, String code, String name) {
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

    void change(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    UUID getId() { return id; }
    String getCode() { return code; }
    String getName() { return name; }
    boolean isActive() { return active; }
}