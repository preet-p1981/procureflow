package com.procureflow.requisition.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.procureflow.catalog.application.port.DepartmentStore;
import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.requisition.application.RequisitionSummary;
import com.procureflow.requisition.application.port.RequisitionQuery;
import com.procureflow.requisition.domain.PurchaseRequisition;
import com.procureflow.requisition.domain.RequisitionLine;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.requisition.domain.port.RequisitionNumberGenerator;
import com.procureflow.requisition.domain.port.RequisitionRepository;
import com.procureflow.shared.application.PageResult;
import com.procureflow.shared.domain.Money;
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
class JpaRequisitionQueryTest {

    private static final Currency INR = Currency.getInstance("INR");

    @Autowired RequisitionQuery query;
    @Autowired RequisitionRepository requisitions;
    @Autowired RequisitionNumberGenerator numbers;
    @Autowired UserRepository users;
    @Autowired DepartmentStore departments;

    private UUID alice;
    private UUID bob;
    private UUID departmentId;

    @BeforeEach
    void setUp() {
        alice = newUser();
        bob = newUser();
        String unique = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        departmentId = UUID.fromString(departments.insert("Q" + unique, "Query test").id().toString());
    }

    private UUID newUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return users.save(User.register(EmailAddress.of(unique + "@example.com"), "hash", "User", Set.of()))
                .id().value();
    }

    private PurchaseRequisition save(UUID requester, boolean submit) {
        RequisitionLine line = new RequisitionLine(1, null, "Pen", BigDecimal.TEN, "box", Money.of("5", "INR"));
        PurchaseRequisition r = PurchaseRequisition.createDraft(numbers.next(2026), requester, departmentId,
                INR, "Need pens", List.of(line));
        if (submit) {
            r.submit(Instant.parse("2026-11-02T10:00:00Z"));
        }
        return requisitions.save(r);
    }

    @Test
    void returnsOnlyTheRequestersOwnRecords() {
        save(alice, false);
        save(alice, true);
        save(bob, false);

        PageResult<RequisitionSummary> page = query.find(alice, null, 0, 20);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).allMatch(s -> s.requesterId().equals(alice));
    }

    @Test
    void filtersByStatus() {
        save(alice, false);
        PurchaseRequisition pending = save(alice, true);

        PageResult<RequisitionSummary> page = query.find(alice, RequisitionStatus.PENDING_APPROVAL, 0, 20);
        assertThat(page.content()).extracting(RequisitionSummary::id).containsExactly(pending.id());
        assertThat(page.content().get(0).total()).isEqualByComparingTo("50");
    }

    @Test
    void pagesAndOrdersNewestFirst() {
        for (int i = 0; i < 3; i++) {
            save(alice, false);
        }
        PageResult<RequisitionSummary> first = query.find(alice, null, 0, 2);
        PageResult<RequisitionSummary> second = query.find(alice, null, 1, 2);

        assertThat(first.content()).hasSize(2);
        assertThat(second.content()).hasSize(1);
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content().get(0).createdAt())
                .isAfterOrEqualTo(first.content().get(1).createdAt());   // non-increasing = newest first
    }

    @Test
    void noFilterReturnsEveryonesRecords() {
        PurchaseRequisition a = save(alice, false);
        PurchaseRequisition b = save(bob, false);

        PageResult<RequisitionSummary> page = query.find(null, null, 0, 100);
        assertThat(page.content()).extracting(RequisitionSummary::id).contains(a.id(), b.id());
    }
}