package com.procureflow.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget")
public class BudgetEntity {

    @Id
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "allocated", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocated;

    @Column(name = "committed", nullable = false, precision = 19, scale = 4)
    private BigDecimal committed;

    @Column(name = "consumed", nullable = false, precision = 19, scale = 4)
    private BigDecimal consumed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected BudgetEntity() {
    }

    BudgetEntity(UUID id, UUID departmentId, int fiscalYear, String currency,
                 BigDecimal allocated, BigDecimal committed, BigDecimal consumed) {
        this.id = id;
        this.departmentId = departmentId;
        this.fiscalYear = fiscalYear;
        this.currency = currency;
        this.allocated = allocated;
        this.committed = committed;
        this.consumed = consumed;
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
    UUID getDepartmentId() { return departmentId; }
    int getFiscalYear() { return fiscalYear; }
    String getCurrency() { return currency; }
    BigDecimal getAllocated() { return allocated; }
    BigDecimal getCommitted() { return committed; }
    BigDecimal getConsumed() { return consumed; }
    Long getVersion() { return version; }
}