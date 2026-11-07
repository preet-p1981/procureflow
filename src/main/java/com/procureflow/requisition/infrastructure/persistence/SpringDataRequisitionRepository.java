package com.procureflow.requisition.infrastructure.persistence;

import com.procureflow.requisition.domain.RequisitionStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRequisitionRepository extends JpaRepository<RequisitionEntity, UUID> {

    Page<RequisitionEntity> findByRequesterId(UUID requesterId, Pageable pageable);

    Page<RequisitionEntity> findByRequesterIdAndStatus(UUID requesterId, RequisitionStatus status, Pageable pageable);

    Page<RequisitionEntity> findByStatus(RequisitionStatus status, Pageable pageable);
}