package com.procureflow.requisition.application;

import java.util.List;
import java.util.UUID;

public record CreateRequisitionCommand(UUID departmentId, String currency, String justification,
                                       List<RequisitionLineInput> lines) {
}