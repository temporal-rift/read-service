package io.github.temporalrift.read.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ObjectMapper objectMapper, BearerTokenResolver bearerTokenResolver) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver)
                        .authenticationEntryPoint(new UnauthorizedEntryPoint(objectMapper))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new PlayerAuthenticationConverter())))
                .build();
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        return new WebSocketBearerTokenResolver();
    }
}
