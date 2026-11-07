package com.procureflow.requisition.infrastructure.persistence;

import com.procureflow.requisition.application.RequisitionSummary;
import com.procureflow.requisition.application.port.RequisitionQuery;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.shared.application.PageResult;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class JpaRequisitionQuery implements RequisitionQuery {

    private final SpringDataRequisitionRepository repo;

    JpaRequisitionQuery(SpringDataRequisitionRepository repo) {
        this.repo = repo;
    }

    @Override
    public PageResult<RequisitionSummary> find(UUID requesterId, RequisitionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<RequisitionEntity> result;
        if (requesterId != null && status != null) {
            result = repo.findByRequesterIdAndStatus(requesterId, status, pageable);
        } else if (requesterId != null) {
            result = repo.findByRequesterId(requesterId, pageable);
        } else if (status != null) {
            result = repo.findByStatus(status, pageable);
        } else {
            result = repo.findAll(pageable);
        }

        return new PageResult<>(
                result.getContent().stream().map(JpaRequisitionQuery::toSummary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static RequisitionSummary toSummary(RequisitionEntity e) {
        // the lines collection is lazy and never touched here, so no extra queries per row
        return new RequisitionSummary(e.getId(), e.getNumber(), e.getRequesterId(), e.getDepartmentId(),
                e.getCurrency(), e.getTotalEstimate(), e.getStatus(), e.getSubmittedAt(), e.getCreatedAt());
    }
}