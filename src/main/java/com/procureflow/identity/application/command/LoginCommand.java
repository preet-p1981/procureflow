package com.procureflow.identity.application.command;

public record LoginCommand(String email, String rawPassword) {}