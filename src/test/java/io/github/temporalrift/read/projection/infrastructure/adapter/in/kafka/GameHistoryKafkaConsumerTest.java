package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class GameHistoryKafkaConsumerTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Mock
    GameHistoryEventApplier applier;

    @Mock
    ObjectMapper objectMapper;

    @Test
    void supportedGameEvent_claimsThenDispatches() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-history")).willReturn(true);

        consumer().handleGameEvent(KafkaTestMessages.withEventIdAndEventType(eventId, "EventsDrawn"));

        then(processedEvents).should().claim(eventId, "projection.game-history");
        then(applier).should().applyEventsDrawn(any());
    }

    @Test
    void handDealt_claimsThenDispatches() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-history")).willReturn(true);

        consumer().handleGameEvent(KafkaTestMessages.withEventIdAndEventType(eventId, "HandDealt"));

        then(applier).should().applyHandDealt(any());
    }

    @Test
    void supportedTimelineEvent_claimsThenDispatches() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-history")).willReturn(true);

        consumer().handleTimelineEvent(KafkaTestMessages.withEventIdAndEventType(eventId, "ParadoxCascaded"));

        then(applier).should().applyParadoxCascaded(any());
    }

    @Test
    void unrelatedEvent_isFilteredBeforeClaim() {
        consumer().handleGameEvent(KafkaTestMessages.withEventIdAndEventType(UUID.randomUUID(), "GameStarted"));

        verifyNoInteractions(processedEvents, applier);
    }

    @Test
    void duplicateDelivery_doesNotDispatch() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-history")).willReturn(false);

        consumer().handleTimelineEvent(KafkaTestMessages.withEventIdAndEventType(eventId, "OutcomeApplied"));

        verifyNoInteractions(applier);
    }

    private GameHistoryKafkaConsumer consumer() {
        return new GameHistoryKafkaConsumer(processedEvents, applier, objectMapper);
    }
}
