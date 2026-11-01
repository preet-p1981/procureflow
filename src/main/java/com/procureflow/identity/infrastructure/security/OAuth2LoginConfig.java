package com.procureflow.identity.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class OAuth2LoginConfig {

    // Only the browser handshake with Google. The API chain in SecurityConfig stays stateless.
    @Bean
    @Order(1)
    SecurityFilterChain oauth2LoginChain(HttpSecurity http, GoogleLoginSuccessHandler success) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .oauth2Login(o -> o
                        .successHandler(success)
                        .failureHandler((req, res, ex) ->
                                GoogleLoginSuccessHandler.writeError(res, 401, "EXTERNAL_LOGIN_FAILED")));
        return http.build();
    }
}