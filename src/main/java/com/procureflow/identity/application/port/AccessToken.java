package com.procureflow.identity.application.port;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt){

}
