package com.procureflow.vendor.domain;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import java.util.Objects;

public class Vendor {

    private final VendorId id;
    private final TaxId taxId;
    private final long version;

    private String legalName;
    private String contactEmail;
    private VendorStatus status;
    private String statusReason;

    private Vendor(VendorId id, String legalName, TaxId taxId, String contactEmail,
                   VendorStatus status, String statusReason, long version) {
        this.id = Objects.requireNonNull(id);
        this.taxId = Objects.requireNonNull(taxId);
        this.legalName = requireText(legalName, "Legal name", 200);
        this.contactEmail = requireEmail(contactEmail);
        this.status = Objects.requireNonNull(status);
        this.statusReason = statusReason;
        this.version = version;
    }

    /** A new vendor always starts PENDING and needs review. */
    public static Vendor register(String legalName, TaxId taxId, String contactEmail) {
        return new Vendor(VendorId.newId(), legalName, taxId, contactEmail,
                VendorStatus.PENDING, null, 0L);
    }

    /** Rebuilds a vendor loaded from storage. Used only by the persistence adapter. */
    public static Vendor reconstitute(VendorId id, String legalName, TaxId taxId, String contactEmail,
                                      VendorStatus status, String statusReason, long version) {
        return new Vendor(id, legalName, taxId, contactEmail, status, statusReason, version);
    }

    // ---- state machine ----

    public void approve() {
        requireStatus(VendorStatus.PENDING, VendorStatus.APPROVED);
        status = VendorStatus.APPROVED;
        statusReason = null;
    }

    public void reject(String reason) {
        requireStatus(VendorStatus.PENDING, VendorStatus.REJECTED);
        status = VendorStatus.REJECTED;
        statusReason = requireText(reason, "Reason", 500);
    }

    public void suspend(String reason) {
        requireStatus(VendorStatus.APPROVED, VendorStatus.SUSPENDED);
        status = VendorStatus.SUSPENDED;
        statusReason = requireText(reason, "Reason", 500);
    }

    public void reinstate() {
        requireStatus(VendorStatus.SUSPENDED, VendorStatus.APPROVED);
        status = VendorStatus.APPROVED;
        statusReason = null;
    }

    public boolean canReceiveOrders() {
        return status == VendorStatus.APPROVED;
    }

    // ---- read access ----

    public VendorId id() { return id; }
    public TaxId taxId() { return taxId; }
    public String legalName() { return legalName; }
    public String contactEmail() { return contactEmail; }
    public VendorStatus status() { return status; }
    public String statusReason() { return statusReason; }
    public long version() { return version; }

    // ---- helpers ----

    private void requireStatus(VendorStatus expected, VendorStatus target) {
        if (status != expected) {
            throw new DomainException(VendorError.VENDOR_INVALID_TRANSITION,
                    "Cannot move vendor from " + status + " to " + target);
        }
    }

    private static String requireText(String value, String name, int max) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CommonError.VALIDATION_FAILED, name + " must not be blank");
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new DomainException(CommonError.VALIDATION_FAILED, name + " is too long");
        }
        return v;
    }

    private static String requireEmail(String value) {
        String v = requireText(value, "Contact email", 254);
        if (!v.contains("@") || v.startsWith("@") || v.endsWith("@")) {
            throw new DomainException(CommonError.VALIDATION_FAILED, "Contact email is not valid");
        }
        return v;
    }
}