package io.github.temporalrift.read.notification.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.application.port.in.FanOutNotificationUseCase;
import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationPolicy;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;

@Service
class NotificationFanOutService implements FanOutNotificationUseCase {

    private final NotificationPolicy policy;
    private final NotificationSessionRegistry sessions;
    private final ObjectMapper objectMapper;

    NotificationFanOutService(
            NotificationPolicy policy, NotificationSessionRegistry sessions, ObjectMapper objectMapper) {
        this.policy = policy;
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    @Override
    public void fanOut(Message<Object> message) {
        var eventType = header(message, "eventType");
        var gameId = uuidHeader(message, "gameId");
        if (eventType == null || gameId == null) {
            return;
        }
        var delivery = policy.deliveryFor(eventType);
        if (delivery == NotificationPolicy.Delivery.NEVER) {
            return;
        }
        var payload = payload(message.getPayload());
        var notification = NotificationMessage.event(eventType, occurredAt(message), payload);
        var targetPlayerId = delivery == NotificationPolicy.Delivery.TARGETED ? uuid(payload, "playerId") : null;
        if (delivery == NotificationPolicy.Delivery.TARGETED && targetPlayerId == null) {
            return;
        }
        sessions.sessionsFor(gameId).stream()
                .filter(session ->
                        targetPlayerId == null || session.recipient().playerId().equals(targetPlayerId))
                .forEach(session -> {
                    try {
                        session.deliver(notification);
                    } catch (RuntimeException e) {
                        sessions.unregister(session.sessionId());
                    }
                });
    }

    private JsonNode payload(Object value) {
        return value instanceof byte[] bytes ? objectMapper.readTree(bytes) : objectMapper.valueToTree(value);
    }

    private static UUID uuidHeader(Message<?> message, String name) {
        return uuid(header(message, name));
    }

    private static UUID uuid(JsonNode payload, String name) {
        return payload.hasNonNull(name) ? uuid(payload.get(name).asText()) : null;
    }

    private static UUID uuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant occurredAt(Message<?> message) {
        var raw = header(message, "occurredAt");
        try {
            return raw == null ? null : Instant.parse(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String header(Message<?> message, String name) {
        return switch (message.getHeaders().get(name)) {
            case String value -> value;
            case byte[] value -> new String(value, StandardCharsets.UTF_8);
            case null, default -> null;
        };
    }
}
