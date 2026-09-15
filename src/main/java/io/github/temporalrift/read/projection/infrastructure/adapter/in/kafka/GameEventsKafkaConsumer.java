package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.InboundEventClaim;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.MessageHeaders;

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

    // Must be every type on game.events, not just the ones ProjectionEventApplier projects — narrowing this
    // would misclassify a legitimate, still-unprojected type as unsupported and skip it without claiming.
    private static final Set<String> KNOWN_EVENT_TYPES = Set.of(
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.LOBBY_CREATED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PLAYER_JOINED_LOBBY_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PLAYER_LEFT_LOBBY_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HOST_TRANSFERRED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.LOBBY_CLOSED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FACTIONS_DRAWN_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FACTION_ASSIGNED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GAME_STARTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GAME_START_FAILED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GAME_START_CANCELLED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PLAYER_DISCONNECTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PLAYER_ABANDONED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ERA_STARTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ERA_ENDED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ERA_FAILED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GAME_ENDED_ABNORMALLY_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GAME_ENDED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TIMELINE_COLLAPSED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TIMELINE_STABILIZED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.WIN_CONDITION_MET_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FACTION_REVEALED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EVENTS_DRAWN_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HAND_DEALT_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HAND_SELECTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.RESOLUTION_STARTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ACTION_ROUND_STARTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CARD_PLAYED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract
                    .PARADOX_RESOLUTION_CARD_PLAYED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SPECIAL_ACTION_PLAYED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PLAYER_JAMMED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.INFLUENCE_TRACED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HAND_CARD_INTERCEPTED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ACTION_ROUND_TIMER_EXPIRED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PLAYER_SKIPPED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ACTION_ROUND_CLOSED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ROUND_SUMMARY_PUBLISHED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract
                    .BANDED_PROBABILITY_PUBLISHED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract
                    .ACTIVIST_DECLARATION_RECORDED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.EXPOSE_SIGNATURE_REVEALED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.EXPOSE_BEHAVIOR_CHANGED_EVENT_TYPE,
            io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.SCORES_UPDATED_EVENT_TYPE);

    private final ProcessedEventPort processedEvents;
    private final SessionEventDispatcher session;
    private final ActionEventDispatcher action;
    private final ScoringEventDispatcher scoring;
    private final ObjectMapper objectMapper;
    private final KafkaSkipMetrics skipMetrics;

    GameEventsKafkaConsumer(
            ProcessedEventPort processedEvents,
            ProjectionEventApplier applier,
            ObjectMapper objectMapper,
            KafkaSkipMetrics skipMetrics) {
        this.processedEvents = processedEvents;
        this.session = new SessionEventDispatcher(applier);
        this.action = new ActionEventDispatcher(applier);
        this.scoring = new ScoringEventDispatcher(applier);
        this.objectMapper = objectMapper;
        this.skipMetrics = skipMetrics;
    }

    @KafkaListener(topics = "game.events", groupId = "read-service." + CONSUMER)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(Message<Object> message) {
        if (UnsupportedEventGate.isUnsupported(
                message, CONSUMER, KNOWN_EVENT_TYPES, UnsupportedEventGate.CURRENT_ENVELOPE_VERSION, skipMetrics)) {
            return;
        }
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
