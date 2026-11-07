package com.procureflow.requisition.domain.port;

import com.procureflow.requisition.domain.PurchaseRequisition;
import java.util.Optional;
import java.util.UUID;

public interface RequisitionRepository {
    Optional<PurchaseRequisition> findById(UUID id);
    PurchaseRequisition save(PurchaseRequisition requisition);
}