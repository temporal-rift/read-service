package io.github.temporalrift.read.notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket.NotificationDeliveryFactory;
import io.github.temporalrift.read.notification.infrastructure.adapter.out.inmemory.DiscardingNotificationDeliveryAdapter;
import io.github.temporalrift.read.notification.infrastructure.adapter.out.websocket.WebSocketNotificationDeliveryAdapter;

@Configuration
class NotificationDeliveryConfiguration {

    @Bean
    @Profile("!in-memory")
    NotificationDeliveryFactory webSocketNotificationDelivery(
            ObjectMapper objectMapper, NotificationWebSocketProperties properties) {
        return session -> new WebSocketNotificationDeliveryAdapter(
                new ConcurrentWebSocketSessionDecorator(
                        session, properties.sendTimeLimitMillis(), properties.sendBufferSizeBytes()),
                objectMapper);
    }

    @Bean
    @Profile("in-memory")
    NotificationDeliveryFactory discardingNotificationDelivery() {
        return session -> new DiscardingNotificationDeliveryAdapter();
    }
}
