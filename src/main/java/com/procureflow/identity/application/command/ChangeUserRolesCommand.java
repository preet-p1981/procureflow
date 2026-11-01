package com.procureflow.identity.application.command;

import java.util.List;
import java.util.UUID;

public record ChangeUserRolesCommand(UUID userId, List<String> assign, List<String> remove) {}