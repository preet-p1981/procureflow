package com.procureflow.vendor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

class VendorTest {

    private Vendor newVendor() {
        return Vendor.register("Acme Pvt Ltd", TaxId.of("24ABCDE1234F1Z5"), "sales@acme.com");
    }

    @Test
    void newVendorIsPending() {
        Vendor v = newVendor();
        assertThat(v.status()).isEqualTo(VendorStatus.PENDING);
        assertThat(v.canReceiveOrders()).isFalse();
    }

    @Test
    void approveThenSuspendThenReinstate() {
        Vendor v = newVendor();
        v.approve();
        assertThat(v.canReceiveOrders()).isTrue();

        v.suspend("Compliance review");
        assertThat(v.status()).isEqualTo(VendorStatus.SUSPENDED);
        assertThat(v.statusReason()).isEqualTo("Compliance review");

        v.reinstate();
        assertThat(v.status()).isEqualTo(VendorStatus.APPROVED);
        assertThat(v.statusReason()).isNull();
    }

    @Test
    void rejectedVendorCannotBeApproved() {
        Vendor v = newVendor();
        v.reject("Documents missing");

        assertThatThrownBy(v::approve)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode().code())
                .isEqualTo("VENDOR_INVALID_TRANSITION");
    }

    @Test
    void pendingVendorCannotBeSuspended() {
        assertThatThrownBy(() -> newVendor().suspend("why"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectNeedsAReason() {
        assertThatThrownBy(() -> newVendor().reject(" "))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void taxIdIsNormalisedAndValidated() {
        assertThat(TaxId.of(" 24abcde1234f1z5 ").value()).isEqualTo("24ABCDE1234F1Z5");
        assertThatThrownBy(() -> TaxId.of("12-3")).isInstanceOf(DomainException.class);
    }
}