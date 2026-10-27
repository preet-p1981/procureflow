package com.procureflow.identity.domain;

import com.procureflow.shared.domain.DomainException;
import java.util.Locale;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final int MAX_LENGTH = 255;
    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new DomainException(IdentityError.INVALID_EMAIL, "Email must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
            throw new DomainException(IdentityError.INVALID_EMAIL, "Invalid email address");
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    @Override
    public String toString() {
        return value;
    }
}