package io.github.temporalrift.read.notification.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.application.port.in.ConnectNotificationSessionUseCase;
import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationRecipient;
import io.github.temporalrift.read.notification.domain.model.NotificationSession;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;

@Service
class NotificationSessionService implements ConnectNotificationSessionUseCase {

    private final GetPlayerGameStateUseCase playerGameState;
    private final NotificationSessionRegistry sessions;
    private final ObjectMapper objectMapper;

    NotificationSessionService(
            GetPlayerGameStateUseCase playerGameState,
            NotificationSessionRegistry sessions,
            ObjectMapper objectMapper) {
        this.playerGameState = playerGameState;
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    @Override
    public void connect(String sessionId, UUID gameId, UUID playerId, NotificationDeliveryPort delivery) {
        var snapshot = playerGameState.get(gameId, playerId);
        delivery.send(new NotificationMessage("SNAPSHOT", null, null, objectMapper.valueToTree(snapshot)));
        sessions.register(new NotificationSession(sessionId, new NotificationRecipient(gameId, playerId), delivery));
    }

    @Override
    public void disconnect(String sessionId) {
        sessions.unregister(sessionId);
    }
}
