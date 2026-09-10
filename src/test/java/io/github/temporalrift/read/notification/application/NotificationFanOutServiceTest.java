package io.github.temporalrift.read.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationPolicy;
import io.github.temporalrift.read.notification.domain.model.NotificationRecipient;
import io.github.temporalrift.read.notification.domain.model.NotificationSession;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

class NotificationFanOutServiceTest {

    @Test
    void broadcastsParadoxCascadeButNeverForwardsResolutionBarrier() {
        var gameId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ParadoxCascaded", "{}"));
        verify(recipient).send(any());

        service.fanOut(message(gameId, "EraResolutionCompleted", "{}"));
        verify(recipient, times(1)).send(any());
    }

    @Test
    void targetsHandDealtToPayloadPlayerOnly() {
        var gameId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var target = mock(NotificationDeliveryPort.class);
        var other = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("target", gameId, targetPlayerId, target));
        registry.register(activeSession("other", gameId, UUID.randomUUID(), other));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "HandDealt", "{\"playerId\":\"" + targetPlayerId + "\"}"));

        verify(target).send(any());
        verify(other, never()).send(any());
    }

    @Test
    void targetsProbabilityStateRevealedToTheScanningPlayerOnly() {
        var gameId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var viewer = mock(NotificationDeliveryPort.class);
        var otherOne = mock(NotificationDeliveryPort.class);
        var otherTwo = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("viewer", gameId, viewerId, viewer));
        registry.register(activeSession("other-one", gameId, UUID.randomUUID(), otherOne));
        registry.register(activeSession("other-two", gameId, UUID.randomUUID(), otherTwo));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ProbabilityStateRevealed", "{\"playerId\":\"" + viewerId + "\"}"));

        verify(viewer).send(any());
        verify(otherOne, never()).send(any());
        verify(otherTwo, never()).send(any());
    }

    @Test
    void targetsPlayerJammedToTheSuppressedPlayerOnly() {
        var gameId = UUID.randomUUID();
        var suppressedPlayerId = UUID.randomUUID();
        var suppressed = mock(NotificationDeliveryPort.class);
        var otherOne = mock(NotificationDeliveryPort.class);
        var otherTwo = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("suppressed", gameId, suppressedPlayerId, suppressed));
        registry.register(activeSession("other-one", gameId, UUID.randomUUID(), otherOne));
        registry.register(activeSession("other-two", gameId, UUID.randomUUID(), otherTwo));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "PlayerJammed", "{\"playerId\":\"" + suppressedPlayerId + "\"}"));

        verify(suppressed).send(any());
        verify(otherOne, never()).send(any());
        verify(otherTwo, never()).send(any());
    }

    @Test
    void dropsTargetedEventWithoutPlayerId() {
        var gameId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "HandDealt", "{}"));

        verify(recipient, never()).send(any());
    }

    @Test
    void continuesDeliveringWhenOneSessionFails() {
        var gameId = UUID.randomUUID();
        var failing = mock(NotificationDeliveryPort.class);
        var healthy = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("failing", gameId, UUID.randomUUID(), failing));
        registry.register(activeSession("healthy", gameId, UUID.randomUUID(), healthy));
        doThrow(new IllegalStateException("closed")).when(failing).send(any());
        doThrow(new IllegalStateException("already closed")).when(failing).close();
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ParadoxCascaded", "{}"));

        verify(healthy).send(any());
        assertThat(registry.sessionsFor(gameId)).hasSize(1);
    }

    private static Message<Object> message(UUID gameId, String type, String payload) {
        return MessageBuilder.withPayload((Object) payload.getBytes())
                .setHeader("gameId", gameId.toString())
                .setHeader("eventType", type)
                .build();
    }

    private static NotificationSession activeSession(
            String sessionId, UUID gameId, UUID playerId, NotificationDeliveryPort delivery) {
        var session = new NotificationSession(sessionId, new NotificationRecipient(gameId, playerId), delivery, 256);
        session.activate(new NotificationMessage("SNAPSHOT", null, null, null));
        clearInvocations(delivery);
        return session;
    }
}
