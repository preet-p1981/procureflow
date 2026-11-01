package com.procureflow.identity.domain;

import java.time.Duration;

public record RefreshTokenPolicy(Duration ttl) {}