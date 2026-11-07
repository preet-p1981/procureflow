package com.procureflow.requisition.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateRequisitionRequest(
        @Size(max = 1000) String justification,
        @Size(max = 100) List<@Valid RequisitionLineRequest> lines) {
}