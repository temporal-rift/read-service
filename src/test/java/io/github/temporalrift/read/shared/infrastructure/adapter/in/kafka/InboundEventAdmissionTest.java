package io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import io.github.temporalrift.read.shared.InboundRecord;
import io.github.temporalrift.read.shared.InboundScope;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class InboundEventAdmissionTest {

    private static final String CONSUMER = "test-consumer";

    @Mock
    ProcessedEventPort processedEvents;

    @Mock
    InboundScope scope;

    @Test
    void admit_missingEventId_discardedWithoutClaiming() {
        var admission = new InboundEventAdmission(scope, processedEvents);

        var result = admission.admit(message(null), CONSUMER);

        assertThat(result).isEmpty();
        verify(processedEvents, never()).claim(any(), anyString());
    }

    @Test
    void admit_nonUuidEventId_discardedWithoutClaiming() {
        var admission = new InboundEventAdmission(scope, processedEvents);

        var result = admission.admit(message("not-a-uuid"), CONSUMER);

        assertThat(result).isEmpty();
        verify(processedEvents, never()).claim(any(), anyString());
    }

    @Test
    void admit_newEvent_claimsAndReturnsEventId() {
        var eventId = UUID.randomUUID();
        given(scope.admits(any())).willReturn(true);
        given(processedEvents.claim(eventId, CONSUMER)).willReturn(true);

        var result = new InboundEventAdmission(scope, processedEvents).admit(message(eventId.toString()), CONSUMER);

        assertThat(result).contains(eventId);
    }

    @Test
    void admit_duplicateEvent_returnsEmpty() {
        var eventId = UUID.randomUUID();
        given(scope.admits(any())).willReturn(true);
        given(processedEvents.claim(eventId, CONSUMER)).willReturn(false);

        var result = new InboundEventAdmission(scope, processedEvents).admit(message(eventId.toString()), CONSUMER);

        assertThat(result).isEmpty();
    }

    @Test
    void admit_eventIdHeaderAsRawBytes_claimsAndReturnsEventId() {
        var eventId = UUID.randomUUID();
        given(scope.admits(any())).willReturn(true);
        given(processedEvents.claim(eventId, CONSUMER)).willReturn(true);
        var message = MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8))
                .build();

        var result = new InboundEventAdmission(scope, processedEvents).admit(message, CONSUMER);

        assertThat(result).contains(eventId);
    }

    @Test
    void admit_recordOutsideTheScope_ignoredWithoutClaiming() {
        given(scope.admits(any())).willReturn(false);

        var result = new InboundEventAdmission(scope, processedEvents)
                .admit(message(UUID.randomUUID().toString()), CONSUMER);

        assertThat(result).isEmpty();
        verify(processedEvents, never()).claim(any(), anyString());
    }

    @Test
    void admit_passesTheRecordSourcePositionAndGameToTheScope() {
        var gameId = UUID.randomUUID();
        var message = MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", UUID.randomUUID().toString())
                .setHeader("gameId", gameId.toString())
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "game.events")
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 2)
                .setHeader(KafkaHeaders.OFFSET, 41L)
                .build();

        new InboundEventAdmission(scope, processedEvents).admit(message, CONSUMER);

        verify(scope).admits(new InboundRecord("game.events", 2, 41L, gameId));
    }

    private static Message<Object> message(String eventId) {
        return MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", eventId)
                .build();
    }
}
