package com.procureflow.vendor.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterVendorRequest(
        @NotBlank @Size(max = 200) String legalName,
        @NotBlank @Size(max = 30) String taxId,
        @NotBlank @Email @Size(max = 254) String contactEmail) {}