package io.github.temporalrift.read.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

class NotificationSessionTest {

    @Test
    void deliversSnapshotBeforeNotificationsReceivedWhileItWasInitializing() {
        var delivered = new ArrayList<NotificationMessage>();
        NotificationDeliveryPort delivery = delivered::add;
        var session = new NotificationSession(
                "session", new NotificationRecipient(UUID.randomUUID(), UUID.randomUUID()), delivery, 2);
        var liveEvent = NotificationMessage.event("ParadoxCascaded", null, null);
        var snapshot = new NotificationMessage("SNAPSHOT", null, null, null);

        session.deliver(liveEvent);
        session.activate(snapshot);

        assertThat(delivered).containsExactly(snapshot, liveEvent);
    }

    @Test
    void rejectsNotificationsBeyondTheInitializationBufferLimit() {
        NotificationDeliveryPort delivery = message -> {};
        var session = new NotificationSession(
                "session", new NotificationRecipient(UUID.randomUUID(), UUID.randomUUID()), delivery, 1);

        session.deliver(NotificationMessage.event("ParadoxCascaded", null, null));

        assertThatThrownBy(() -> session.deliver(NotificationMessage.event("OutcomeApplied", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Notification session initialization buffer is full");
    }
}
