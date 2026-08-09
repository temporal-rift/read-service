package io.github.temporalrift.read.notification.application;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
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
    private final int maxPendingMessages;

    NotificationSessionService(
            GetPlayerGameStateUseCase playerGameState,
            NotificationSessionRegistry sessions,
            ObjectMapper objectMapper,
            @Value("${notification.websocket.max-pending-messages}") int maxPendingMessages) {
        this.playerGameState = playerGameState;
        this.sessions = sessions;
        this.objectMapper = objectMapper;
        if (maxPendingMessages < 1) {
            throw new IllegalArgumentException("notification.websocket.max-pending-messages must be positive");
        }
        this.maxPendingMessages = maxPendingMessages;
    }

    @Override
    public void connect(String sessionId, UUID gameId, UUID playerId, NotificationDeliveryPort delivery) {
        var session = new NotificationSession(
                sessionId, new NotificationRecipient(gameId, playerId), delivery, maxPendingMessages);
        sessions.register(session);
        try {
            var snapshot = playerGameState.get(gameId, playerId);
            session.activate(new NotificationMessage("SNAPSHOT", null, null, objectMapper.valueToTree(snapshot)));
        } catch (RuntimeException e) {
            sessions.unregister(sessionId);
            delivery.close();
            throw e;
        }
    }

    @Override
    public void disconnect(String sessionId) {
        sessions.unregister(sessionId);
    }
}
