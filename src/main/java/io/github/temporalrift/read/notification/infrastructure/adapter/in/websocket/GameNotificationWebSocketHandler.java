package io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.application.port.in.ConnectNotificationSessionUseCase;
import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

@Component
public class GameNotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConnectNotificationSessionUseCase sessions;
    private final ObjectMapper objectMapper;

    GameNotificationWebSocketHandler(ConnectNotificationSessionUseCase sessions, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        var principal = session.getPrincipal();
        if (!(principal instanceof PlayerAuthenticationToken token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            sessions.connect(
                    session.getId(),
                    gameId(session),
                    token.getPrincipal().playerId(),
                    message -> send(session, message));
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

    private void send(WebSocketSession session, Object message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to deliver WebSocket notification", e);
        }
    }

    private static UUID gameId(WebSocketSession session) {
        var path = session.getUri().getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }
}
