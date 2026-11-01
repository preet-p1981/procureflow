package com.procureflow.identity.application;

import java.util.List;

public record UserRoles(String id, String email, List<String> roles) {
}
