package com.procureflow.requisition.infrastructure.persistence;

import com.procureflow.requisition.domain.PurchaseRequisition;
import com.procureflow.requisition.domain.RequisitionLine;
import com.procureflow.requisition.domain.port.RequisitionRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
class JpaRequisitionRepository implements RequisitionRepository {

    private final SpringDataRequisitionRepository repo;

    JpaRequisitionRepository(SpringDataRequisitionRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PurchaseRequisition> findById(UUID id) {
        return repo.findById(id).map(JpaRequisitionRepository::toDomain);
    }

    @Override
    public PurchaseRequisition save(PurchaseRequisition r) {
        RequisitionEntity entity = repo.findById(r.id()).orElse(null);
        boolean isNew = entity == null;

        if (isNew) {
            entity = new RequisitionEntity(r.id(), r.number(), r.requesterId(), r.departmentId(),
                    r.currency().getCurrencyCode());
        } else if (entity.getVersion() != r.version()) {
            throw new DomainException(CommonError.CONFLICT,
                    "Requisition was modified by someone else, reload and try again");
        }

        entity.applyHeader(r.justification(), r.total().amount(), r.status(),
                r.rejectionReason(), r.submittedAt(), r.decidedAt());

        if (!isNew) {
            // delete the old lines first, otherwise re-inserting line_no 1 would hit the unique constraint
            entity.getLines().clear();
            repo.flush();
        }
        for (RequisitionLine line : r.lines()) {
            entity.addLine(new RequisitionLineEntity(line.lineNo(), line.itemId(), line.description(),
                    line.quantity(), line.unitOfMeasure(), line.unitPrice().amount()));
        }

        if (isNew) {
            repo.saveAndFlush(entity);      // new row: INSERT
        } else {
            repo.flush();                   // managed row: Hibernate already tracks the changes
        }
        return toDomain(entity);
    }

    private static PurchaseRequisition toDomain(RequisitionEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());
        List<RequisitionLine> lines = e.getLines().stream()
                .map(l -> new RequisitionLine(l.getLineNo(), l.getItemId(), l.getDescription(),
                        l.getQuantity(), l.getUnitOfMeasure(), new Money(l.getUnitPrice(), currency)))
                .toList();
        return PurchaseRequisition.reconstitute(e.getId(), e.getNumber(), e.getRequesterId(), e.getDepartmentId(),
                currency, e.getJustification(), lines, e.getStatus(), e.getRejectionReason(),
                e.getSubmittedAt(), e.getDecidedAt(), e.getVersion() == null ? 0L : e.getVersion());
    }
}