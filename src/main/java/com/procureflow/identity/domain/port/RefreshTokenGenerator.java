package com.procureflow.identity.domain.port;

public interface RefreshTokenGenerator {
    String generateRaw();          // random string given to the client
    String hash(String rawToken);  // what we store
}