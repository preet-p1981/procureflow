package com.procureflow.config;

import com.procureflow.identity.domain.LockoutPolicy;
import java.time.Clock;
import java.time.Duration;

import com.procureflow.identity.domain.RefreshTokenPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityBeansConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LockoutPolicy lockoutPolicy() {
        return new LockoutPolicy(5, Duration.ofMinutes(15));
    }
    @Bean
    RefreshTokenPolicy refreshTokenPolicy(
            @org.springframework.beans.factory.annotation.Value("${procureflow.security.refresh-ttl-days}") long days) {
        return new RefreshTokenPolicy(java.time.Duration.ofDays(days));
    }
}