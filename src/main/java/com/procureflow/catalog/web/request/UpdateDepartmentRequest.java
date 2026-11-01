package com.procureflow.catalog.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(@NotBlank @Size(max = 150) String name, boolean active) {}