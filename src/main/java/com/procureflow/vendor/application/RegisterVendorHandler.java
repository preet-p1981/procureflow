package com.procureflow.vendor.application;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.vendor.domain.TaxId;
import com.procureflow.vendor.domain.Vendor;
import com.procureflow.vendor.domain.VendorError;
import com.procureflow.vendor.domain.port.VendorRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterVendorHandler {

    private final VendorRepository vendors;

    public RegisterVendorHandler(VendorRepository vendors) {
        this.vendors = vendors;
    }

    @Transactional
    @PreAuthorize("hasAuthority('VENDOR_MANAGE')")
    public VendorView handle(String legalName, String taxId, String contactEmail) {
        TaxId tax = TaxId.of(taxId);
        if (vendors.existsByTaxId(tax)) {
            throw new DomainException(VendorError.TAX_ID_ALREADY_REGISTERED, "Tax id is already registered");
        }
        return VendorView.from(vendors.save(Vendor.register(legalName, tax, contactEmail)));
    }
}