package io.github.temporalrift.read.notification.domain.port.out;

import io.github.temporalrift.read.notification.domain.model.NotificationMessage;

public interface NotificationDeliveryPort {

    void send(NotificationMessage message);
}
