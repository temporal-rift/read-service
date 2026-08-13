package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolutionPhaseStartedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolvedPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.InboundEventClaim;

/** Consumes {@code timeline.events} (resolution facts from timeline-service) — design.md Decision 2/9. */
@Component
class TimelineEventsKafkaConsumer {

    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String CONSUMER = "projection.timeline-events";

    private final ProcessedEventPort processedEvents;
    private final ProjectionEventApplier applier;
    private final ObjectMapper objectMapper;

    TimelineEventsKafkaConsumer(
            ProcessedEventPort processedEvents, ProjectionEventApplier applier, ObjectMapper objectMapper) {
        this.processedEvents = processedEvents;
        this.applier = applier;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "timeline.events", groupId = "read-service." + CONSUMER)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(Message<Object> message) {
        InboundEventClaim.accept(message, CONSUMER, processedEvents).ifPresent(eventId -> dispatch(message));
    }

    private void dispatch(Message<Object> message) {
        var eventType = MessageHeaders.asString(message, EVENT_TYPE_HEADER);
        switch (eventType) {
            case null -> {
                // No eventType header — nothing to dispatch.
            }
            case "OutcomeApplied" -> applier.applyOutcomeApplied(read(message, OutcomeAppliedPayload.class));
            case "ParadoxResolutionPhaseStarted" ->
                applier.applyParadoxResolutionPhaseStarted(read(message, ParadoxResolutionPhaseStartedPayload.class));
            case "ParadoxResolved" -> applier.applyParadoxResolved(read(message, ParadoxResolvedPayload.class));
            case "ParadoxCascaded" -> applier.applyParadoxCascaded(read(message, ParadoxCascadedPayload.class));
            default -> {
                // Other timeline.events types aren't consumed by this projection.
            }
        }
    }

    private <T> T read(Message<Object> message, Class<T> type) {
        return GameEventPayloads.read(objectMapper, message.getPayload(), type);
    }
}
