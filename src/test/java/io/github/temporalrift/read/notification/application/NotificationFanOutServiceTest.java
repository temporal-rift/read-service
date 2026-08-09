package io.github.temporalrift.read.notification.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
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
        var recipient = org.mockito.Mockito.mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ParadoxCascaded", "{}"));
        verify(recipient).send(org.mockito.ArgumentMatchers.any());

        service.fanOut(message(gameId, "EraResolutionCompleted", "{}"));
        verify(recipient, org.mockito.Mockito.times(1)).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void targetsHandDealtToPayloadPlayerOnly() {
        var gameId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var target = org.mockito.Mockito.mock(NotificationDeliveryPort.class);
        var other = org.mockito.Mockito.mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("target", gameId, targetPlayerId, target));
        registry.register(activeSession("other", gameId, UUID.randomUUID(), other));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "HandDealt", "{\"playerId\":\"" + targetPlayerId + "\"}"));

        verify(target).send(org.mockito.ArgumentMatchers.any());
        verify(other, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dropsTargetedEventWithoutPlayerId() {
        var gameId = UUID.randomUUID();
        var recipient = org.mockito.Mockito.mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "HandDealt", "{}"));

        verify(recipient, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void continuesDeliveringWhenOneSessionFails() {
        var gameId = UUID.randomUUID();
        var failing = org.mockito.Mockito.mock(NotificationDeliveryPort.class);
        var healthy = org.mockito.Mockito.mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("failing", gameId, UUID.randomUUID(), failing));
        registry.register(activeSession("healthy", gameId, UUID.randomUUID(), healthy));
        org.mockito.Mockito.doThrow(new IllegalStateException("closed"))
                .when(failing)
                .send(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doThrow(new IllegalStateException("already closed"))
                .when(failing)
                .close();
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ParadoxCascaded", "{}"));

        verify(healthy).send(org.mockito.ArgumentMatchers.any());
        org.assertj.core.api.Assertions.assertThat(registry.sessionsFor(gameId)).hasSize(1);
    }

    private static org.springframework.messaging.Message<Object> message(UUID gameId, String type, String payload) {
        return MessageBuilder.withPayload((Object) payload.getBytes())
                .setHeader("gameId", gameId.toString())
                .setHeader("eventType", type)
                .build();
    }

    private static NotificationSession activeSession(
            String sessionId, UUID gameId, UUID playerId, NotificationDeliveryPort delivery) {
        var session = new NotificationSession(sessionId, new NotificationRecipient(gameId, playerId), delivery, 256);
        session.activate(new NotificationMessage("SNAPSHOT", null, null, null));
        org.mockito.Mockito.clearInvocations(delivery);
        return session;
    }
}
