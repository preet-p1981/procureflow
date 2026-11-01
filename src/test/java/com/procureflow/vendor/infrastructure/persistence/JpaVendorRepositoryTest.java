package com.procureflow.vendor.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.shared.domain.DomainException;
import com.procureflow.vendor.domain.TaxId;
import com.procureflow.vendor.domain.Vendor;
import com.procureflow.vendor.domain.VendorStatus;
import com.procureflow.vendor.domain.port.VendorRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaVendorRepositoryTest {

    @Autowired VendorRepository vendors;

    private Vendor newVendor() {
        String tax = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        return Vendor.register("Acme " + tax, TaxId.of(tax), "sales@acme.com");
    }

    @Test
    void savedVendorIsFoundWithSameData() {
        Vendor v = vendors.save(newVendor());

        var found = vendors.findById(v.id()).orElseThrow();

        assertThat(found.legalName()).isEqualTo(v.legalName());
        assertThat(found.status()).isEqualTo(VendorStatus.PENDING);
        assertThat(vendors.existsByTaxId(v.taxId())).isTrue();
    }

    @Test
    void statusChangeIsPersistedAndFilterable() {
        Vendor v = vendors.save(newVendor());
        v = vendors.findById(v.id()).orElseThrow();
        v.approve();
        vendors.save(v);

        assertThat(vendors.findByStatus(VendorStatus.APPROVED))
                .extracting(Vendor::id).contains(v.id());
    }

    @Test
    void duplicateTaxIdIsRejected() {
        Vendor first = vendors.save(newVendor());
        Vendor dup = Vendor.register("Other Ltd", first.taxId(), "x@y.com");

        assertThatThrownBy(() -> vendors.save(dup))
                .isInstanceOf(DomainException.class);
    }
}