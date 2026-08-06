package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.port.out.ProcessedEventPort;

/**
 * Consumes {@code game.events} (session/action/scoring facts from game-service), dispatching by the
 * stable {@code eventType} header.
 */
@Component
class GameEventsKafkaConsumer {

    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String CONSUMER = "projection.game-events";

    private final ProcessedEventPort processedEvents;
    private final ProjectionEventApplier applier;
    private final ObjectMapper objectMapper;

    GameEventsKafkaConsumer(
            ProcessedEventPort processedEvents, ProjectionEventApplier applier, ObjectMapper objectMapper) {
        this.processedEvents = processedEvents;
        this.applier = applier;
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
        switch (eventType) {
            case "GameStarted" -> applier.applyGameStarted(read(message, GameStartedPayload.class));
            case "FactionAssigned" -> applier.applyFactionAssigned(read(message, FactionAssignedPayload.class));
            case "EraStarted" -> applier.applyEraStarted(read(message, EraStartedPayload.class));
            case "EventsDrawn" -> applier.applyEventsDrawn(read(message, EventsDrawnPayload.class));
            case "HandDealt" -> applier.applyHandDealt(read(message, HandDealtPayload.class));
            case "PlayerDisconnected" ->
                applier.applyPlayerDisconnected(read(message, PlayerDisconnectedPayload.class));
            case "PlayerAbandoned" -> applier.applyPlayerAbandoned(read(message, PlayerAbandonedPayload.class));
            case "EraEnded" -> applier.applyEraEnded(read(message, EraEndedPayload.class));
            case "GameEnded" -> applier.applyGameEnded(read(message, GameEndedPayload.class));
            case "FactionRevealed" -> applier.applyFactionRevealed(read(message, FactionRevealedPayload.class));
            case "ResolutionStarted" -> applier.applyResolutionStarted(read(message, ResolutionStartedPayload.class));
            case "ActionRoundStarted" ->
                applier.applyActionRoundStarted(read(message, ActionRoundStartedPayload.class));
            case "CardPlayed" -> applier.applyCardPlayed(read(message, CardPlayedPayload.class));
            case "ScoresUpdated" -> applier.applyScoresUpdated(read(message, ScoresUpdatedPayload.class));
            default -> {
                // Not consumed by this slice.
            }
        }
    }

    private <T> T read(Message<Object> message, Class<T> type) {
        return GameEventPayloads.read(objectMapper, message.getPayload(), type);
    }
}
