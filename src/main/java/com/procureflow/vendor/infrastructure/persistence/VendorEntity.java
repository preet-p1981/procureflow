package com.procureflow.vendor.infrastructure.persistence;

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
@Table(name = "vendor")
class VendorEntity {

    @Id
    private UUID id;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "tax_id", nullable = false, unique = true, length = 30)
    private String taxId;

    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VendorEntity() {}

    VendorEntity(UUID id, String legalName, String taxId, String contactEmail,
                 String status, String statusReason, long version) {
        this.id = id;
        this.legalName = legalName;
        this.taxId = taxId;
        this.contactEmail = contactEmail;
        this.status = status;
        this.statusReason = statusReason;
        this.version = version;
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
    String getLegalName() { return legalName; }
    String getTaxId() { return taxId; }
    String getContactEmail() { return contactEmail; }
    String getStatus() { return status; }
    String getStatusReason() { return statusReason; }
    long getVersion() { return version; }
}