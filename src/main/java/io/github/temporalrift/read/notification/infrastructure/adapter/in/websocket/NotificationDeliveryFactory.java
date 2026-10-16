package io.github.temporalrift.read.notification.infrastructure.adapter.in.websocket;

import org.springframework.web.socket.WebSocketSession;

import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

/** Opens the delivery a connected WebSocket session receives its notifications through. */
@FunctionalInterface
public interface NotificationDeliveryFactory {

    NotificationDeliveryPort open(WebSocketSession session);
}
