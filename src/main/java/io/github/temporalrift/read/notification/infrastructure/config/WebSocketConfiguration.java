package io.github.temporalrift.read.notification.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket.GameNotificationWebSocketHandler;

@Configuration
@EnableWebSocket
@EnableConfigurationProperties(NotificationWebSocketProperties.class)
class WebSocketConfiguration implements WebSocketConfigurer {

    private final GameNotificationWebSocketHandler handler;
    private final NotificationWebSocketProperties properties;

    WebSocketConfiguration(GameNotificationWebSocketHandler handler, NotificationWebSocketProperties properties) {
        this.handler = handler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/games/{gameId}")
                .setAllowedOriginPatterns(properties.allowedOrigins().toArray(String[]::new));
    }
}
