package com.procureflow.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "procureflow.security.jwt")
public record JwtProperties(String secret,Long accessTtlMinutes , String issuer){
}
