package com.procureflow.vendor.infrastructure.persistence;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.vendor.domain.TaxId;
import com.procureflow.vendor.domain.Vendor;
import com.procureflow.vendor.domain.VendorError;
import com.procureflow.vendor.domain.VendorId;
import com.procureflow.vendor.domain.VendorStatus;
import com.procureflow.vendor.domain.port.VendorRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaVendorRepository implements VendorRepository {

    private final SpringDataVendorRepository jpa;

    JpaVendorRepository(SpringDataVendorRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Vendor> findById(VendorId id) {
        return jpa.findById(id.value()).map(JpaVendorRepository::toDomain);
    }

    @Override
    public boolean existsByTaxId(TaxId taxId) {
        return jpa.existsByTaxId(taxId.value());
    }

    @Override
    public List<Vendor> findByStatus(VendorStatus status) {
        return jpa.findByStatusOrderByLegalName(status.name()).stream()
                .map(JpaVendorRepository::toDomain).toList();
    }

    @Override
    @Transactional
    public Vendor save(Vendor v) {
        try {
            VendorEntity saved = jpa.saveAndFlush(new VendorEntity(v.id().value(), v.legalName(),
                    v.taxId().value(), v.contactEmail(), v.status().name(), v.statusReason(), v.version()));
            return toDomain(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new DomainException(CommonError.CONFLICT, "Vendor was modified by someone else, reload and retry");
        } catch (DataIntegrityViolationException e) {
            throw new DomainException(VendorError.TAX_ID_ALREADY_REGISTERED, "Tax id is already registered");
        }
    }

    private static Vendor toDomain(VendorEntity e) {
        return Vendor.reconstitute(new VendorId(e.getId()), e.getLegalName(), new TaxId(e.getTaxId()),
                e.getContactEmail(), VendorStatus.valueOf(e.getStatus()), e.getStatusReason(), e.getVersion());
    }
}