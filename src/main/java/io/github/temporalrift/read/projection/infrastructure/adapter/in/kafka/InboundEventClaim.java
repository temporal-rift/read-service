package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import io.github.temporalrift.read.projection.domain.port.out.ProcessedEventPort;

/**
 * Shared idempotency prologue for the {@code game.events}/{@code timeline.events} consumers: read the real Kafka
 * header {@code eventId} (event-schema.md §1 — not a body field), discard a missing one, then claim it for the
 * given logical consumer. Extracted up front to avoid the exact duplication SonarCloud flagged across
 * timeline-service's near-identical consumers (fixed there by {@code GameEventIngestion}).
 */
final class InboundEventClaim {

    private static final Logger log = LoggerFactory.getLogger(InboundEventClaim.class);

    private InboundEventClaim() {}

    /**
     * @return the claimed {@code eventId}, or empty if the envelope was malformed or already claimed by
     *     {@code consumer} (the reason, if any, is already logged here)
     */
    static Optional<UUID> accept(Message<?> message, String consumer, ProcessedEventPort processedEvents) {
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
