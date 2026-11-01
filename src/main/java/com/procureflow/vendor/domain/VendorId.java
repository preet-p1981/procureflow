package com.procureflow.vendor.domain;

import java.util.Objects;
import java.util.UUID;

public record VendorId(UUID value) {

    public VendorId {
        Objects.requireNonNull(value);
    }

    public static VendorId newId() {
        return new VendorId(UUID.randomUUID());
    }
}