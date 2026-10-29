package io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.shared.InboundRecord;
import io.github.temporalrift.read.shared.InboundScope;
import io.github.temporalrift.read.shared.ProcessedEventPort;

/**
 * Admission prologue of every read-service Kafka consumer: a record is handled only when it is well formed, inside
 * this deployment's {@link InboundScope}, and newly claimed for the consumer. A record outside the scope is consumed
 * without a claim, so it leaves no state behind.
 */
@Component
public class InboundEventAdmission {

    private static final Logger log = LoggerFactory.getLogger(InboundEventAdmission.class);

    private final InboundScope scope;
    private final ProcessedEventPort processedEvents;

    public InboundEventAdmission(InboundScope scope, ProcessedEventPort processedEvents) {
        this.scope = scope;
        this.processedEvents = processedEvents;
    }

    public Optional<UUID> admit(Message<?> message, String consumer) {
        var eventId = uuidOf(message, "eventId");
        if (eventId == null) {
            log.warn("Malformed envelope (missing or invalid eventId) for consumer {} — discarding", consumer);
            return Optional.empty();
        }
        if (!scope.admits(inboundRecordOf(message))) {
            log.debug("Event {} outside the inbound scope ignored by consumer {}", eventId, consumer);
            return Optional.empty();
        }
        if (!processedEvents.claim(eventId, consumer)) {
            log.debug("Duplicate event {} ignored by consumer {}", eventId, consumer);
            return Optional.empty();
        }
        return Optional.of(eventId);
    }

    private static InboundRecord inboundRecordOf(Message<?> message) {
        var headers = message.getHeaders();
        return new InboundRecord(
                headers.get(KafkaHeaders.RECEIVED_TOPIC) instanceof String topic ? topic : null,
                headers.get(KafkaHeaders.RECEIVED_PARTITION) instanceof Integer partition ? partition : null,
                headers.get(KafkaHeaders.OFFSET) instanceof Long offset ? offset : null,
                uuidOf(message, "gameId"));
    }

    private static UUID uuidOf(Message<?> message, String header) {
        var raw = MessageHeaders.asString(message, header);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}
