package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

public abstract class NotificationDeliveryContract {

    protected abstract NotificationDeliveryPort delivery();

    @Test
    void aNotificationIsAcceptedWithoutFailure() {
        var message = NotificationMessage.event(
                "PhaseChanged", Instant.parse("2026-08-01T00:00:00Z"), new ObjectMapper().createObjectNode());

        assertThatCode(() -> delivery().send(message)).doesNotThrowAnyException();
    }
}
