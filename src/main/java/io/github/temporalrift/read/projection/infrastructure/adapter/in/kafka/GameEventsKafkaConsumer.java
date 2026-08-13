package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.InboundEventClaim;

/**
 * Consumes {@code game.events} — session/action/scoring facts from game-service, spread across three
 * independently-owned {@code apis} contract modules that all publish to this one Kafka topic. Composes their
 * generated dispatchers (design.md "Migration addendum: consumer contract adoption") rather than collapsing
 * them into one generated artifact, so Kafka topic layout doesn't dictate dependency/code boundaries.
 */
@Component
class GameEventsKafkaConsumer {

    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String CONSUMER = "projection.game-events";

    private final ProcessedEventPort processedEvents;
    private final SessionEventDispatcher session;
    private final ActionEventDispatcher action;
    private final ScoringEventDispatcher scoring;
    private final ObjectMapper objectMapper;

    GameEventsKafkaConsumer(
            ProcessedEventPort processedEvents, ProjectionEventApplier applier, ObjectMapper objectMapper) {
        this.processedEvents = processedEvents;
        this.session = new SessionEventDispatcher(applier);
        this.action = new ActionEventDispatcher(applier);
        this.scoring = new ScoringEventDispatcher(applier);
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "game.events", groupId = "read-service." + CONSUMER)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(Message<Object> message) {
        InboundEventClaim.accept(message, CONSUMER, processedEvents).ifPresent(eventId -> dispatch(message));
    }

    private void dispatch(Message<Object> message) {
        var eventType = MessageHeaders.asString(message, EVENT_TYPE_HEADER);
        if (eventType == null) {
            return;
        }
        var payload = message.getPayload();
        if (session.dispatch(
                eventType,
                payload,
                headers(
                        message,
                        eventType,
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventHeaders::new),
                this::deserialize)) {
            return;
        }
        if (action.dispatch(
                eventType,
                payload,
                headers(
                        message,
                        eventType,
                        io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.EventHeaders::new),
                this::deserialize)) {
            return;
        }
        if (scoring.dispatch(
                eventType,
                payload,
                headers(
                        message,
                        eventType,
                        io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.EventHeaders::new),
                this::deserialize)) {
            return;
        }
        // dispatch() returning false means "not this contract family," not "safe to ignore" — an eventType
        // none of session/action/scoring recognizes is a real failure at the Kafka boundary (design.md
        // "Migration addendum: consumer contract adoption"), not a slice this projection simply skips.
        throw new IllegalArgumentException("Unknown eventType: " + eventType);
    }

    private <T> T deserialize(Object rawPayload, Class<T> type) {
        return GameEventPayloads.read(objectMapper, rawPayload, type);
    }

    private static <H> H headers(Message<Object> message, String eventType, EventHeadersFactory<H> factory) {
        return factory.create(
                eventType,
                asUuid(MessageHeaders.asString(message, "eventId")),
                asUuid(MessageHeaders.asString(message, "aggregateId")),
                MessageHeaders.asString(message, "aggregateType"),
                asUuid(MessageHeaders.asString(message, "gameId")),
                asInstant(MessageHeaders.asString(message, "occurredAt")),
                asInt(MessageHeaders.asString(message, "version")));
    }

    private static UUID asUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private static Instant asInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static int asInt(String value) {
        return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     * Bridges one parsed header set to whichever of {@code session}/{@code action}/{@code scoring}'s own
     * generated {@code EventHeaders} record is needed — the three are structurally identical but distinct
     * types (one per independently-owned {@code apis} module), so a single shared instance isn't possible.
     */
    @FunctionalInterface
    private interface EventHeadersFactory<H> {
        H create(
                String eventType,
                UUID eventId,
                UUID aggregateId,
                String aggregateType,
                UUID gameId,
                Instant occurredAt,
                int version);
    }
}
