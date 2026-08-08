package io.github.temporalrift.read.notification.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket.GameNotificationWebSocketHandler;

@Configuration
@EnableWebSocket
class WebSocketConfiguration implements WebSocketConfigurer {

    private final GameNotificationWebSocketHandler handler;

    WebSocketConfiguration(GameNotificationWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/games/{gameId}").setAllowedOriginPatterns("*");
    }
}
