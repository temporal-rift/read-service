package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.port.out.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class TimelineEventsKafkaConsumerTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Test
    void handle_newEvent_claimsForTimelineEventsConsumer() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);

        new TimelineEventsKafkaConsumer(processedEvents).handle(KafkaTestMessages.withEventId(eventId));

        then(processedEvents).should().claim(eventId, "projection.timeline-events");
    }
}
