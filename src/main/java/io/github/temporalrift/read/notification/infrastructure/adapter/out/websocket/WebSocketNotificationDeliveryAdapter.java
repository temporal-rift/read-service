package io.github.temporalrift.read.notification.infrastructure.adapter.out.websocket;

import java.io.IOException;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

public final class WebSocketNotificationDeliveryAdapter implements NotificationDeliveryPort {

    private final WebSocketSession session;
    private final ObjectMapper objectMapper;

    public WebSocketNotificationDeliveryAdapter(WebSocketSession session, ObjectMapper objectMapper) {
        this.session = session;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(NotificationMessage message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to deliver WebSocket notification", e);
        }
    }

    @Override
    public void close() {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to close WebSocket notification session", e);
        }
    }
}
