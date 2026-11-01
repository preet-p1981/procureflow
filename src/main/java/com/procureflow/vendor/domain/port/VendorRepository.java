package com.procureflow.vendor.domain.port;

import com.procureflow.vendor.domain.TaxId;
import com.procureflow.vendor.domain.Vendor;
import com.procureflow.vendor.domain.VendorId;
import com.procureflow.vendor.domain.VendorStatus;

import java.util.List;
import java.util.Optional;

public interface VendorRepository {
    Optional<Vendor> findById(VendorId id);
    boolean existsByTaxId(TaxId taxId);
    List<Vendor> findByStatus(VendorStatus status);
    Vendor save(Vendor vendor);
}
