package com.procureflow.catalog.application;

import java.util.UUID;

public record CategoryView(UUID id, String code, String name, boolean active) {
}