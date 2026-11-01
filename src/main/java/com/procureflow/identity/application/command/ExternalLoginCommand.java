package com.procureflow.identity.application.command;

public record ExternalLoginCommand(String provider, String subject, String email,
                                   boolean emailVerified, String fullName) {}