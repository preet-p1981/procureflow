package com.procureflow.catalog.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}") String code,
        @NotBlank @Size(max = 150) String name) {}