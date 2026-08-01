package io.github.temporalrift.read;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Overriding the {@code JwtDecoder} bean stops Spring Boot's issuer-uri autoconfiguration from eagerly
 * fetching the (fake, test-only) issuer's discovery document at context startup. Tests never present a real
 * bearer token — they inject {@code Authentication} directly via
 * {@code SecurityMockMvcRequestPostProcessors.authentication(...)}.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            throw new JwtException("Test decoder — inject auth via SecurityMockMvcRequestPostProcessors");
        };
    }
}
