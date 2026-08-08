package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.domain.port.out.ProcessedEventPort;

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
}
