package com.procureflow.identity.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static SecretKey key(JwtProperties p){
        return new SecretKeySpec(p.secret().getBytes(StandardCharsets.UTF_8),"HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties p){
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(p)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties p){
        return NimbusJwtDecoder.withSecretKey(key(p)).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
