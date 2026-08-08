package io.github.temporalrift.read.notification.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

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
        registry.register(
                new NotificationSession("session", new NotificationRecipient(gameId, UUID.randomUUID()), recipient));
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
        registry.register(new NotificationSession("target", new NotificationRecipient(gameId, targetPlayerId), target));
        registry.register(
                new NotificationSession("other", new NotificationRecipient(gameId, UUID.randomUUID()), other));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "HandDealt", "{\"playerId\":\"" + targetPlayerId + "\"}"));

        verify(target).send(org.mockito.ArgumentMatchers.any());
        verify(other, never()).send(org.mockito.ArgumentMatchers.any());
    }

    private static org.springframework.messaging.Message<Object> message(UUID gameId, String type, String payload) {
        return MessageBuilder.withPayload((Object) payload.getBytes())
                .setHeader("gameId", gameId.toString())
                .setHeader("eventType", type)
                .build();
    }
}
