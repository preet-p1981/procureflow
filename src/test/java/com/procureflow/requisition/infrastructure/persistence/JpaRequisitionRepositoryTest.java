package com.procureflow.requisition.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.requisition.domain.PurchaseRequisition;
import com.procureflow.requisition.domain.RequisitionLine;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.requisition.domain.port.RequisitionNumberGenerator;
import com.procureflow.requisition.domain.port.RequisitionRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import com.procureflow.shared.domain.Money;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaRequisitionRepositoryTest {

    private static final Currency INR = Currency.getInstance("INR");
    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    @Autowired RequisitionRepository requisitions;
    @Autowired RequisitionNumberGenerator numbers;
    @Autowired UserRepository users;
    @Autowired DepartmentStore departments;
    @Autowired EntityManager em;

    private UUID requesterId;
    private UUID departmentId;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        requesterId = users.save(User.register(EmailAddress.of(unique + "@example.com"), "hash",
                "Requester", Set.of())).id().value();
        departmentId = UUID.fromString(departments.insert("D" + unique.toUpperCase(), "Test department").id().toString());    }

    private static RequisitionLine line(int no, String qty, String price) {
        return new RequisitionLine(no, null, "Ballpoint pen", new BigDecimal(qty), "box", Money.of(price, "INR"));
    }

    private PurchaseRequisition newDraft(RequisitionLine... lines) {
        return PurchaseRequisition.createDraft(numbers.next(2026), requesterId, departmentId, INR,
                "Team needs stationery", List.of(lines));
    }

    private PurchaseRequisition reload(UUID id) {
        em.flush();
        em.clear();                       // forget cached objects so the next read really hits the database
        return requisitions.findById(id).orElseThrow();
    }

    @Test
    void savesAndLoadsHeaderAndLines() {
        PurchaseRequisition saved = requisitions.save(newDraft(line(1, "10", "12.50"), line(2, "2", "100")));

        PurchaseRequisition loaded = reload(saved.id());
        assertThat(loaded.number()).matches("PR-2026-\\d{6}");
        assertThat(loaded.status()).isEqualTo(RequisitionStatus.DRAFT);
        assertThat(loaded.lines()).hasSize(2);
        assertThat(loaded.lines().get(0).lineNo()).isEqualTo(1);
        assertThat(loaded.total()).isEqualTo(Money.of("325", "INR"));
        assertThat(loaded.requesterId()).isEqualTo(requesterId);
    }

    @Test
    void persistsStatusChanges() {
        PurchaseRequisition r = requisitions.save(newDraft(line(1, "1", "50")));
        r.submit(NOW);
        requisitions.save(r);

        PurchaseRequisition loaded = reload(r.id());
        assertThat(loaded.status()).isEqualTo(RequisitionStatus.PENDING_APPROVAL);
        assertThat(loaded.submittedAt()).isEqualTo(NOW);
    }

    @Test
    void replacingLinesReusesLineNumbersSafely() {
        PurchaseRequisition r = requisitions.save(newDraft(line(1, "1", "10"), line(2, "1", "20")));
        r.updateDraft("Changed", List.of(line(1, "5", "30")));
        requisitions.save(r);

        PurchaseRequisition loaded = reload(r.id());
        assertThat(loaded.justification()).isEqualTo("Changed");
        assertThat(loaded.lines()).hasSize(1);
        assertThat(loaded.total()).isEqualTo(Money.of("150", "INR"));
    }

    @Test
    void everySaveIncreasesTheVersion() {
        PurchaseRequisition first = requisitions.save(newDraft(line(1, "1", "10")));
        first.updateDraft("Same total, other text", List.of(line(1, "1", "10")));
        PurchaseRequisition second = requisitions.save(first);
        assertThat(second.version()).isGreaterThan(first.version());
    }

    @Test
    void rejectsStaleVersion() {
        PurchaseRequisition saved = requisitions.save(newDraft(line(1, "1", "10")));
        PurchaseRequisition copyA = requisitions.findById(saved.id()).orElseThrow();
        PurchaseRequisition copyB = requisitions.findById(saved.id()).orElseThrow();

        copyA.updateDraft("A was here", copyA.lines());
        requisitions.save(copyA);

        copyB.updateDraft("B was here", copyB.lines());
        assertThatThrownBy(() -> requisitions.save(copyB))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).errorCode())
                .isEqualTo(CommonError.CONFLICT);
    }

    @Test
    void numberGeneratorGivesIncreasingFormattedNumbers() {
        String a = numbers.next(2026);
        String b = numbers.next(2026);
        assertThat(a).matches("PR-2026-\\d{6}");
        assertThat(b).isNotEqualTo(a);
        assertThat(b.compareTo(a)).isGreaterThan(0);
    }
}