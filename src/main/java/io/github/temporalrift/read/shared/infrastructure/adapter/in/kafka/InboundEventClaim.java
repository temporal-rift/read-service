package io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import io.github.temporalrift.read.shared.domain.port.out.ProcessedEventPort;

/** Idempotency prologue for read-service Kafka consumers. */
public final class InboundEventClaim {

    private static final Logger log = LoggerFactory.getLogger(InboundEventClaim.class);

    private InboundEventClaim() {}

    public static Optional<UUID> accept(Message<?> message, String consumer, ProcessedEventPort processedEvents) {
        var eventId = eventIdOf(message);
        if (eventId == null) {
            log.warn("Malformed envelope (missing or invalid eventId) for consumer {} — discarding", consumer);
            return Optional.empty();
        }
        if (!processedEvents.claim(eventId, consumer)) {
            log.debug("Duplicate event {} ignored by consumer {}", eventId, consumer);
            return Optional.empty();
        }
        return Optional.of(eventId);
    }

    private static UUID eventIdOf(Message<?> message) {
        var raw = message.getHeaders().get("eventId", String.class);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
