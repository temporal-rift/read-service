package io.github.temporalrift.read.notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.temporalrift.read.notification.domain.model.NotificationPolicy;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;

@Configuration
class NotificationConfiguration {

    @Bean
    NotificationPolicy notificationPolicy() {
        return new NotificationPolicy();
    }

    @Bean
    NotificationSessionRegistry notificationSessionRegistry() {
        return new NotificationSessionRegistry();
    }
}
