package com.procureflow.requisition.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateRequisitionRequest(
        @NotNull UUID departmentId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code") String currency,
        @Size(max = 1000) String justification,
        @Size(max = 100) List<@Valid RequisitionLineRequest> lines) {
}