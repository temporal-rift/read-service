package io.github.temporalrift.read.notification.domain.model;

import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

public record NotificationSession(
        String sessionId, NotificationRecipient recipient, NotificationDeliveryPort delivery) {}
