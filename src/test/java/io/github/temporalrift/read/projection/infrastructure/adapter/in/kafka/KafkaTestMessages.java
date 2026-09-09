package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Builds test messages carrying the {@code eventId} header this MVP1 slice reads (design.md Decision 2) and,
 * optionally, {@code eventType} for dispatch tests. Other envelope headers (aggregateId, aggregateType, gameId,
 * occurredAt, version) are deliberately omitted: the default producer-side header mapper can't JSON-encode
 * {@code Instant} without {@code jackson-datatype-jsr310}, which this slice has no other reason to depend on yet.
 */
final class KafkaTestMessages {

    private KafkaTestMessages() {}

    static Message<Object> withEventId(UUID eventId) {
        return withRawEventId(eventId == null ? null : eventId.toString());
    }

    static Message<Object> withRawEventId(String rawEventId) {
        return MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", rawEventId)
                .build();
    }

    /**
     * Mirrors a header arriving as the raw {@code byte[]} record value — what a non-Spring producer (e.g. a
     * bare {@code KafkaProducer}, bypassing {@code DefaultKafkaHeaderMapper}'s JSON encoding) puts on the
     * wire, as opposed to the already-decoded {@code String} {@link #withEventId} builds.
     */
    static Message<Object> withEventIdBytes(UUID eventId) {
        return MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8))
                .build();
    }

    static Message<Object> withEventIdAndEventType(UUID eventId, Object eventTypeHeaderValue) {
        return MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", eventTypeHeaderValue)
                .build();
    }
}
