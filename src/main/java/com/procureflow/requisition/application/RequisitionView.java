package com.procureflow.requisition.application;

import com.procureflow.requisition.domain.PurchaseRequisition;
import com.procureflow.requisition.domain.RequisitionLine;
import com.procureflow.requisition.domain.RequisitionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequisitionView(UUID id, String number, UUID requesterId, UUID departmentId,
                              String justification, String currency, BigDecimal total,
                              RequisitionStatus status, String rejectionReason,
                              Instant submittedAt, Instant decidedAt, long version,
                              List<LineView> lines) {

    public record LineView(int lineNo, UUID itemId, String description, BigDecimal quantity,
                           String unitOfMeasure, BigDecimal unitPrice, BigDecimal lineTotal) {

        static LineView from(RequisitionLine l) {
            return new LineView(l.lineNo(), l.itemId(), l.description(), l.quantity(), l.unitOfMeasure(),
                    l.unitPrice().amount(), l.lineTotal().amount());
        }
    }

    public static RequisitionView from(PurchaseRequisition r) {
        return new RequisitionView(r.id(), r.number(), r.requesterId(), r.departmentId(), r.justification(),
                r.currency().getCurrencyCode(), r.total().amount(), r.status(), r.rejectionReason(),
                r.submittedAt(), r.decidedAt(), r.version(),
                r.lines().stream().map(LineView::from).toList());
    }
}