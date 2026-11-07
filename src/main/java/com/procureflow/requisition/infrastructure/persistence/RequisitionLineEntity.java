package com.procureflow.requisition.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "requisition_line")
public class RequisitionLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private RequisitionEntity requisition;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    protected RequisitionLineEntity() {
    }

    RequisitionLineEntity(int lineNo, UUID itemId, String description, BigDecimal quantity,
                          String unitOfMeasure, BigDecimal unitPrice) {
        this.id = UUID.randomUUID();
        this.lineNo = lineNo;
        this.itemId = itemId;
        this.description = description;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.unitPrice = unitPrice;
    }

    void setRequisition(RequisitionEntity requisition) { this.requisition = requisition; }

    int getLineNo() { return lineNo; }
    UUID getItemId() { return itemId; }
    String getDescription() { return description; }
    BigDecimal getQuantity() { return quantity; }
    String getUnitOfMeasure() { return unitOfMeasure; }
    BigDecimal getUnitPrice() { return unitPrice; }
}