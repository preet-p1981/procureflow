package com.procureflow.requisition.application.port;

import com.procureflow.requisition.application.RequisitionSummary;
import com.procureflow.requisition.domain.RequisitionStatus;
import com.procureflow.shared.application.PageResult;
import java.util.UUID;

public interface RequisitionQuery {
    /** Newest first. A null requester or status means "no filter". */
    PageResult<RequisitionSummary> find(UUID requesterIdOrNull, RequisitionStatus statusOrNull, int page, int size);
}