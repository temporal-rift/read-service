package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.util.Set;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.InboundEventClaim;

/**
 * Replays history facts from both source topics under one stable logical consumer identity.
 *
 * <p>Unsupported types are filtered before the atomic claim so this consumer records only events that can
 * mutate history.
 */
@Component
class GameHistoryKafkaConsumer {

    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String CONSUMER = "projection.game-history";
    private static final String GROUP_ID = "read-service." + CONSUMER;
    private static final Set<String> GAME_EVENT_TYPES = Set.of("EventsDrawn", "EraEnded", "HandSelected");
    private static final Set<String> TIMELINE_EVENT_TYPES = Set.of("OutcomeApplied", "ParadoxCascaded");

    private final ProcessedEventPort processedEvents;
    private final GameHistoryEventApplier applier;
    private final ObjectMapper objectMapper;

    GameHistoryKafkaConsumer(
            ProcessedEventPort processedEvents, GameHistoryEventApplier applier, ObjectMapper objectMapper) {
        this.processedEvents = processedEvents;
        this.applier = applier;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "game.events", groupId = GROUP_ID)
    @Transactional(propagation = REQUIRES_NEW)
    public void handleGameEvent(Message<Object> message) {
        var eventType = supportedEventType(message, GAME_EVENT_TYPES);
        if (eventType == null) {
            return;
        }
        InboundEventClaim.accept(message, CONSUMER, processedEvents)
                .ifPresent(ignored -> dispatchGameEvent(eventType, message));
    }

    @KafkaListener(topics = "timeline.events", groupId = GROUP_ID)
    @Transactional(propagation = REQUIRES_NEW)
    public void handleTimelineEvent(Message<Object> message) {
        var eventType = supportedEventType(message, TIMELINE_EVENT_TYPES);
        if (eventType == null) {
            return;
        }
        InboundEventClaim.accept(message, CONSUMER, processedEvents)
                .ifPresent(ignored -> dispatchTimelineEvent(eventType, message));
    }

    private String supportedEventType(Message<Object> message, Set<String> supportedTypes) {
        var eventType = MessageHeaders.asString(message, EVENT_TYPE_HEADER);
        return supportedTypes.contains(eventType) ? eventType : null;
    }

    private void dispatchGameEvent(String eventType, Message<Object> message) {
        switch (eventType) {
            case "EventsDrawn" -> applier.applyEventsDrawn(read(message, EventsDrawnPayload.class));
            case "EraEnded" -> applier.applyEraEnded(read(message, EraEndedPayload.class));
            case "HandSelected" -> applier.applyHandSelected(read(message, HandSelectedPayload.class));
            default -> throw new IllegalArgumentException("Unsupported game history event type " + eventType);
        }
    }

    private void dispatchTimelineEvent(String eventType, Message<Object> message) {
        switch (eventType) {
            case "OutcomeApplied" -> applier.applyOutcomeApplied(read(message, OutcomeAppliedPayload.class));
            case "ParadoxCascaded" -> applier.applyParadoxCascaded(read(message, ParadoxCascadedPayload.class));
            default -> throw new IllegalArgumentException("Unsupported game history event type " + eventType);
        }
    }

    private <T> T read(Message<Object> message, Class<T> type) {
        return GameEventPayloads.read(objectMapper, message.getPayload(), type);
    }
}
