package io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.application.port.in.ConnectNotificationSessionUseCase;
import io.github.temporalrift.read.notification.infrastructure.adapter.out.websocket.WebSocketNotificationDeliveryAdapter;
import io.github.temporalrift.read.notification.infrastructure.config.NotificationWebSocketProperties;
import io.github.temporalrift.read.shared.CurrentPlayer;

@Component
public class GameNotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConnectNotificationSessionUseCase sessions;
    private final ObjectMapper objectMapper;
    private final NotificationWebSocketProperties properties;

    GameNotificationWebSocketHandler(
            ConnectNotificationSessionUseCase sessions,
            ObjectMapper objectMapper,
            NotificationWebSocketProperties properties) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        var decoratedSession = new ConcurrentWebSocketSessionDecorator(
                session, properties.sendTimeLimitMillis(), properties.sendBufferSizeBytes());
        try {
            sessions.connect(
                    session.getId(),
                    gameId(session),
                    CurrentPlayer.id(session.getPrincipal()),
                    new WebSocketNotificationDeliveryAdapter(decoratedSession, objectMapper));
        } catch (RuntimeException e) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (!"{\"type\":\"PING\"}".equals(message.getPayload())) {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.disconnect(session.getId());
    }

    private static UUID gameId(WebSocketSession session) {
        var path = session.getUri().getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }
}
