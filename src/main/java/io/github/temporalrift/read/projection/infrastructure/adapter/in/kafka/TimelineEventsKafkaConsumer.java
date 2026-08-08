package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.domain.port.out.ProcessedEventPort;
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
        if ("OutcomeApplied".equals(eventType)) {
            applier.applyOutcomeApplied(
                    GameEventPayloads.read(objectMapper, message.getPayload(), OutcomeAppliedPayload.class));
        }
    }
}
