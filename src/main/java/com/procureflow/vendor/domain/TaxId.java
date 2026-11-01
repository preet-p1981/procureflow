package com.procureflow.vendor.domain;

import com.procureflow.shared.domain.DomainException;

public record TaxId(String value) {

    public TaxId {
        String v = value == null ? "" : value.trim().toUpperCase();
        if (!v.matches("[A-Z0-9]{5,30}")) {
            throw new DomainException(VendorError.INVALID_TAX_ID,
                    "Tax id must be 5-30 letters or digits");
        }
        value = v;
    }

    public static TaxId of(String raw) {
        return new TaxId(raw);
    }
}