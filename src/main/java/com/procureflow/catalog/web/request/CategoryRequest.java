package com.procureflow.catalog.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "letters, digits, - and _ only") String code,
        @NotBlank @Size(max = 150) String name) {
}