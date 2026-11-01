package com.procureflow.vendor.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataVendorRepository extends JpaRepository<VendorEntity, UUID> {
    boolean existsByTaxId(String taxId);
    List<VendorEntity> findByStatusOrderByLegalName(String status);
}