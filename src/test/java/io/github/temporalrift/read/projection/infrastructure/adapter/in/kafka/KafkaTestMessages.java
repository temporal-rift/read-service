package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Builds test messages carrying only the {@code eventId} header — the only envelope field this MVP1 slice reads
 * (design.md Decision 2). Other envelope headers (aggregateId, aggregateType, gameId, occurredAt, version) are
 * deliberately omitted: the default producer-side header mapper can't JSON-encode {@code Instant} without
 * {@code jackson-datatype-jsr310}, which this slice has no other reason to depend on yet.
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
}
