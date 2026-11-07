package com.procureflow.requisition.infrastructure.persistence;

import com.procureflow.requisition.domain.port.RequisitionNumberGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class SequenceRequisitionNumberGenerator implements RequisitionNumberGenerator {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String next(int year) {
        Number value = (Number) em.createNativeQuery("SELECT nextval('requisition_number_seq')").getSingleResult();
        return String.format("PR-%d-%06d", year, value.longValue());
    }
}