package io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import io.github.temporalrift.read.notification.application.port.in.ConnectNotificationSessionUseCase;
import io.github.temporalrift.read.shared.CurrentPlayer;

@Component
public class GameNotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConnectNotificationSessionUseCase sessions;
    private final NotificationDeliveryFactory deliveries;

    GameNotificationWebSocketHandler(
            ConnectNotificationSessionUseCase sessions, NotificationDeliveryFactory deliveries) {
        this.sessions = sessions;
        this.deliveries = deliveries;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        try {
            sessions.connect(
                    session.getId(),
                    gameId(session),
                    CurrentPlayer.id(session.getPrincipal()),
                    deliveries.open(session));
        } catch (RuntimeException _) {
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
