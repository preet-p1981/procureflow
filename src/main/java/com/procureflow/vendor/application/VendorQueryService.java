package com.procureflow.vendor.application;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.vendor.domain.VendorError;
import com.procureflow.vendor.domain.VendorId;
import com.procureflow.vendor.domain.VendorStatus;
import com.procureflow.vendor.domain.port.VendorRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorQueryService {

    private final VendorRepository vendors;

    public VendorQueryService(VendorRepository vendors) {
        this.vendors = vendors;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public VendorView get(UUID id) {
        return vendors.findById(new VendorId(id)).map(VendorView::from)
                .orElseThrow(() -> new DomainException(VendorError.VENDOR_NOT_FOUND, "Vendor not found"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public List<VendorView> listByStatus(VendorStatus status) {
        return vendors.findByStatus(status).stream().map(VendorView::from).toList();
    }
}