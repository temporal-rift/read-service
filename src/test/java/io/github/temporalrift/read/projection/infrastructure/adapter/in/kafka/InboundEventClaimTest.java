package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.InboundEventClaim;

@ExtendWith(MockitoExtension.class)
class InboundEventClaimTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Test
    void accept_missingEventId_discardedWithoutClaiming() {
        var message = KafkaTestMessages.withEventId(null);

        var result = InboundEventClaim.accept(message, "test-consumer", processedEvents);

        assertThat(result).isEmpty();
        verify(processedEvents, never()).claim(any(), anyString());
    }

    @Test
    void accept_nonUuidEventId_discardedWithoutClaiming() {
        var message = KafkaTestMessages.withRawEventId("not-a-uuid");

        var result = InboundEventClaim.accept(message, "test-consumer", processedEvents);

        assertThat(result).isEmpty();
        verify(processedEvents, never()).claim(any(), anyString());
    }

    @Test
    void accept_newEvent_claimsAndReturnsEventId() {
        var eventId = UUID.randomUUID();
        var message = KafkaTestMessages.withEventId(eventId);
        given(processedEvents.claim(eventId, "test-consumer")).willReturn(true);

        var result = InboundEventClaim.accept(message, "test-consumer", processedEvents);

        assertThat(result).contains(eventId);
    }

    @Test
    void accept_duplicateEvent_returnsEmpty() {
        var eventId = UUID.randomUUID();
        var message = KafkaTestMessages.withEventId(eventId);
        given(processedEvents.claim(eventId, "test-consumer")).willReturn(false);

        var result = InboundEventClaim.accept(message, "test-consumer", processedEvents);

        assertThat(result).isEmpty();
    }
}
