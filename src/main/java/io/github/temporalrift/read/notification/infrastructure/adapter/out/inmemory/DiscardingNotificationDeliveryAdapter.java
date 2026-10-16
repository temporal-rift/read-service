package io.github.temporalrift.read.notification.infrastructure.adapter.out.inmemory;

import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

/** Accepts and drops every notification: simulated players hold no socket and read their state over REST. */
public final class DiscardingNotificationDeliveryAdapter implements NotificationDeliveryPort {

    @Override
    public void send(NotificationMessage message) {
        // Intentionally discarded.
    }
}
