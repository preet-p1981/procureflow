package com.procureflow.identity.domain.port;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword,String hash);
}
