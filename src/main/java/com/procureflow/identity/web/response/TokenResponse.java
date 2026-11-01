package com.procureflow.identity.web.response;

import java.time.Instant;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, Instant expiresAt) {}
