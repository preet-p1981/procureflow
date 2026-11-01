package com.procureflow.identity.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRquest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank String password,
        @NotBlank @Size(max = 150) String fullName
) {
}
