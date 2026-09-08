package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class TimelineEventsKafkaConsumerTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Mock
    ProjectionEventApplier applier;

    @Mock
    ObjectMapper objectMapper;

    @Test
    void handle_newEvent_claimsForTimelineEventsConsumer() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper)
                .handle(KafkaTestMessages.withEventId(eventId));

        then(processedEvents).should().claim(eventId, "projection.timeline-events");
    }

    @Test
    void handle_noEventTypeHeader_doesNotDispatch() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper)
                .handle(KafkaTestMessages.withEventId(eventId));

        verifyNoInteractions(applier);
    }

    @Test
    void handle_probabilityStateRevealed_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new ProbabilityStateRevealedPayload(
                UUID.randomUUID(), 2, 1, UUID.randomUUID(), UUID.randomUUID(), List.of());
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ProbabilityStateRevealed")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, ProbabilityStateRevealedPayload.class))
                .willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper).handle(message);

        then(applier).should().applyProbabilityStateRevealed(payload);
    }
}
