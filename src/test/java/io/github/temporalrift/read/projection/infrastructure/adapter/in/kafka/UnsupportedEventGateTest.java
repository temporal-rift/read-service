package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnsupportedEventGateTest {

    private static final Set<String> KNOWN_TYPES = Set.of("GameStarted");

    @Mock
    KafkaSkipMetrics skipMetrics;

    @Test
    void unknownEventType_isUnsupportedAndCounted() {
        var message = KafkaTestMessages.withEventIdAndEventType(UUID.randomUUID(), "SomeFutureEvent");

        var unsupported = UnsupportedEventGate.isUnsupported(message, "consumer", KNOWN_TYPES, 1, skipMetrics);

        assertThat(unsupported).isTrue();
        then(skipMetrics).should().recordUnknownType();
    }

    @Test
    void unsupportedVersion_isUnsupportedAndCounted() {
        var message = KafkaTestMessages.withEventIdAndEventType(UUID.randomUUID(), "GameStarted");

        var unsupported = UnsupportedEventGate.isUnsupported(message, "consumer", KNOWN_TYPES, 2, skipMetrics);

        assertThat(unsupported).isTrue();
        then(skipMetrics).should().recordUnsupportedVersion("consumer");
    }

    @Test
    void missingEventType_isNotUnsupported() {
        var message = KafkaTestMessages.withEventId(UUID.randomUUID());

        var unsupported = UnsupportedEventGate.isUnsupported(message, "consumer", KNOWN_TYPES, 1, skipMetrics);

        assertThat(unsupported).isFalse();
        verifyNoInteractions(skipMetrics);
    }

    @Test
    void knownTypeAndSupportedVersion_isNotUnsupported() {
        var message = KafkaTestMessages.withEventIdAndEventType(UUID.randomUUID(), "GameStarted");

        var unsupported = UnsupportedEventGate.isUnsupported(message, "consumer", KNOWN_TYPES, 1, skipMetrics);

        assertThat(unsupported).isFalse();
        verifyNoInteractions(skipMetrics);
    }

    @Test
    void isUnsupportedVersion_missingVersionHeader_isUnsupported() {
        var message = KafkaTestMessages.withEventId(UUID.randomUUID());

        var unsupported = UnsupportedEventGate.isUnsupportedVersion(message, "consumer", 1, skipMetrics);

        assertThat(unsupported).isTrue();
        then(skipMetrics).should().recordUnsupportedVersion("consumer");
    }
}
