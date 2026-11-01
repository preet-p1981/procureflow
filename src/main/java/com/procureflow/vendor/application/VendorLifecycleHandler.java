package com.procureflow.vendor.application;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.vendor.domain.Vendor;
import com.procureflow.vendor.domain.VendorError;
import com.procureflow.vendor.domain.VendorId;
import com.procureflow.vendor.domain.port.VendorRepository;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorLifecycleHandler {

    private final VendorRepository vendors;

    public VendorLifecycleHandler(VendorRepository vendors) {
        this.vendors = vendors;
    }

    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_MANAGE')")
    public VendorView approve(UUID id) {
        return apply(id, Vendor::approve);
    }

    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_MANAGE')")
    public VendorView reject(UUID id, String reason) {
        return apply(id, v -> v.reject(reason));
    }

    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_MANAGE')")
    public VendorView suspend(UUID id, String reason) {
        return apply(id, v -> v.suspend(reason));
    }

    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_MANAGE')")
    public VendorView reinstate(UUID id) {
        return apply(id, Vendor::reinstate);
    }

    private VendorView apply(UUID id, Consumer<Vendor> action) {
        Vendor vendor = vendors.findById(new VendorId(id))
                .orElseThrow(() -> new DomainException(VendorError.VENDOR_NOT_FOUND, "Vendor not found"));
        action.accept(vendor);
        return VendorView.from(vendors.save(vendor));
    }
}