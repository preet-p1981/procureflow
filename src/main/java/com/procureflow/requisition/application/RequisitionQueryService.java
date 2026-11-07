package com.procureflow.requisition.application;

import com.procureflow.requisition.application.port.CurrentUser;
import com.procureflow.requisition.application.port.RequisitionQuery;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.shared.application.PageResult;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequisitionQueryService {

    static final int MAX_PAGE_SIZE = 100;

    private final RequisitionQuery query;
    private final CurrentUser currentUser;

    public RequisitionQueryService(RequisitionQuery query, CurrentUser currentUser) {
        this.query = query;
        this.currentUser = currentUser;
    }

    /** The logged-in user's own requisitions. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('REQ_READ_OWN', 'REQ_READ_ALL')")
    public PageResult<RequisitionSummary> listMine(RequisitionStatus status, int page, int size) {
        return query.find(currentUser.id(), status, safePage(page), safeSize(size));
    }

    /** Everyone's requisitions, optionally narrowed to one requester. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQ_READ_ALL')")
    public PageResult<RequisitionSummary> listAll(UUID requesterId, RequisitionStatus status, int page, int size) {
        return query.find(requesterId, status, safePage(page), safeSize(size));
    }

    private static int safePage(int page) {
        return Math.max(0, page);
    }

    private static int safeSize(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);   // never let a client ask for 1 million rows
    }
}