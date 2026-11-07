package com.procureflow.requisition.infrastructure.persistence;

import com.procureflow.requisition.domain.RequisitionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_requisition")
public class RequisitionEntity {

    @Id
    private UUID id;

    @Column(name = "number", nullable = false, length = 30, updatable = false)
    private String number;

    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;

    @Column(name = "department_id", nullable = false, updatable = false)
    private UUID departmentId;

    @Column(name = "justification", nullable = false, length = 1000)
    private String justification;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "total_estimate", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalEstimate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequisitionStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<RequisitionLineEntity> lines = new ArrayList<>();

    protected RequisitionEntity() {
    }

    RequisitionEntity(UUID id, String number, UUID requesterId, UUID departmentId, String currency) {
        this.id = id;
        this.number = number;
        this.requesterId = requesterId;
        this.departmentId = departmentId;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    /** Copies the changeable fields. Always touches updated_at so every save counts as a change (and bumps the version). */
    void applyHeader(String justification, BigDecimal totalEstimate, RequisitionStatus status,
                     String rejectionReason, Instant submittedAt, Instant decidedAt) {
        this.justification = justification;
        this.totalEstimate = totalEstimate;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
        this.decidedAt = decidedAt;
        this.updatedAt = Instant.now();
    }

    void addLine(RequisitionLineEntity line) {
        line.setRequisition(this);
        lines.add(line);
    }

    UUID getId() { return id; }
    String getNumber() { return number; }
    UUID getRequesterId() { return requesterId; }
    UUID getDepartmentId() { return departmentId; }
    String getJustification() { return justification; }
    String getCurrency() { return currency; }
    RequisitionStatus getStatus() { return status; }
    String getRejectionReason() { return rejectionReason; }
    Instant getSubmittedAt() { return submittedAt; }
    Instant getDecidedAt() { return decidedAt; }
    Long getVersion() { return version; }
    List<RequisitionLineEntity> getLines() { return lines; }
    java.math.BigDecimal getTotalEstimate() { return totalEstimate; }
    java.time.Instant getCreatedAt() { return createdAt; }
}