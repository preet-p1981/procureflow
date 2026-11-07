package com.procureflow.requisition.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procureflow.requisition.application.port.CurrentUser;
import com.procureflow.requisition.application.port.RequisitionQuery;
import com.procureflow.requisition.domain.RequisitionStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequisitionQueryServiceTest {

    private RequisitionQuery query;
    private RequisitionQueryService service;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        query = mock(RequisitionQuery.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        when(currentUser.id()).thenReturn(me);
        service = new RequisitionQueryService(query, currentUser);
    }

    @Test
    void listMineAlwaysFiltersByTheLoggedInUser() {
        service.listMine(RequisitionStatus.DRAFT, 0, 20);
        verify(query).find(eq(me), eq(RequisitionStatus.DRAFT), eq(0), eq(20));
    }

    @Test
    void listAllCanFilterByRequesterOrNot() {
        UUID other = UUID.randomUUID();
        service.listAll(other, null, 1, 10);
        verify(query).find(eq(other), eq(null), eq(1), eq(10));
        service.listAll(null, null, 0, 10);
        verify(query).find(eq(null), eq(null), eq(0), eq(10));
    }

    @Test
    void pageAndSizeAreClamped() {
        service.listMine(null, -5, 100000);
        verify(query).find(any(), eq(null), eq(0), eq(RequisitionQueryService.MAX_PAGE_SIZE));
        service.listMine(null, 0, 0);
        verify(query).find(any(), eq(null), eq(0), eq(1));
    }
}