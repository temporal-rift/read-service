package io.github.temporalrift.read.notification.application.port.in;

import java.util.UUID;

import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

public interface ConnectNotificationSessionUseCase {

    void connect(String sessionId, UUID gameId, UUID playerId, NotificationDeliveryPort delivery);

    void disconnect(String sessionId);
}
