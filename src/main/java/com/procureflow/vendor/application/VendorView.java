package com.procureflow.vendor.application;

import com.procureflow.vendor.domain.Vendor;

public record VendorView(String id, String legalName, String taxId, String contactEmail,
                         String status, String statusReason) {

    static VendorView from(Vendor v) {
        return new VendorView(v.id().value().toString(), v.legalName(), v.taxId().value(),
                v.contactEmail(), v.status().name(), v.statusReason());
    }
}