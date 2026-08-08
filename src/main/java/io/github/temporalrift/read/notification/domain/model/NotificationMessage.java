package io.github.temporalrift.read.notification.domain.model;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

public record NotificationMessage(String type, String eventType, Instant occurredAt, JsonNode payload) {

    public static NotificationMessage event(String eventType, Instant occurredAt, JsonNode payload) {
        return new NotificationMessage("EVENT", eventType, occurredAt, payload);
    }
}
