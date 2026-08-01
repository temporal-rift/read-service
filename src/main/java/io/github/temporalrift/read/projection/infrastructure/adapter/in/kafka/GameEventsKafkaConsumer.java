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
 * {@code spring.cloud.stream.sendto.destination} binding-name header — design.md Decision 2. Binding names not
 * listed here belong to events this slice doesn't project yet and are silently ignored.
 */
@Component
class GameEventsKafkaConsumer {

    private static final String BINDING_NAME_HEADER = "spring.cloud.stream.sendto.destination";
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
        var bindingName = message.getHeaders().get(BINDING_NAME_HEADER, String.class);
        if (bindingName == null) {
            return;
        }
        switch (bindingName) {
            case "Sessionpublish-game-started-out" -> applier.applyGameStarted(read(message, GameStartedPayload.class));
            case "Sessionpublish-faction-assigned-out" ->
                applier.applyFactionAssigned(read(message, FactionAssignedPayload.class));
            case "Sessionpublish-era-started-out" -> applier.applyEraStarted(read(message, EraStartedPayload.class));
            case "Sessionpublish-events-drawn-out" -> applier.applyEventsDrawn(read(message, EventsDrawnPayload.class));
            case "Sessionpublish-hand-dealt-out" -> applier.applyHandDealt(read(message, HandDealtPayload.class));
            case "Sessionpublish-player-disconnected-out" ->
                applier.applyPlayerDisconnected(read(message, PlayerDisconnectedPayload.class));
            case "Sessionpublish-player-abandoned-out" ->
                applier.applyPlayerAbandoned(read(message, PlayerAbandonedPayload.class));
            case "Sessionpublish-era-ended-out" -> applier.applyEraEnded(read(message, EraEndedPayload.class));
            case "Sessionpublish-game-ended-out" -> applier.applyGameEnded(read(message, GameEndedPayload.class));
            case "Sessionpublish-faction-revealed-out" ->
                applier.applyFactionRevealed(read(message, FactionRevealedPayload.class));
            case "Sessionpublish-resolution-started-out" ->
                applier.applyResolutionStarted(read(message, ResolutionStartedPayload.class));
            case "Actionpublish-action-round-started-out" ->
                applier.applyActionRoundStarted(read(message, ActionRoundStartedPayload.class));
            case "Actionpublish-card-played-out" -> applier.applyCardPlayed(read(message, CardPlayedPayload.class));
            case "Scoringpublish-scores-updated-out" ->
                applier.applyScoresUpdated(read(message, ScoresUpdatedPayload.class));
            default -> {
                // Not consumed by this slice.
            }
        }
    }

    private <T> T read(Message<Object> message, Class<T> type) {
        return GameEventPayloads.read(objectMapper, message.getPayload(), type);
    }
}
