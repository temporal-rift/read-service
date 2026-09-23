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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
    void broadcastsCascadeWithoutExactCarryForwardWeightsToEveryPlayer() {
        var gameId = UUID.randomUUID();
        var first = mock(NotificationDeliveryPort.class);
        var second = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("first", gameId, UUID.randomUUID(), first));
        registry.register(activeSession("second", gameId, UUID.randomUUID(), second));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ParadoxCascaded", """
                {"affectedEventId":"%s","carryForwardProbabilityState":[
                  {"outcomeId":"%s","probability":50}
                ]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID())));

        var firstNotification = ArgumentCaptor.forClass(NotificationMessage.class);
        var secondNotification = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(first).send(firstNotification.capture());
        verify(second).send(secondNotification.capture());
        assertThat(firstNotification.getValue().payload().has("carryForwardProbabilityState"))
                .isFalse();
        assertThat(secondNotification.getValue().payload().has("carryForwardProbabilityState"))
                .isFalse();
    }

    @Test
    void broadcastsCarriedEventDrawWithoutExactWeights() {
        var gameId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "EventsDrawn", """
                {"events":[
                  {"carryOverState":"CASCADED","outcomes":[{"initialProbability":50}]},
                  {"carryOverState":"FRESH","outcomes":[{"initialProbability":34}]}
                ]}
                """));

        var notification = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(recipient).send(notification.capture());
        var events = notification.getValue().payload().get("events");
        assertThat(events.get(0).get("outcomes").get(0).has("initialProbability"))
                .isFalse();
        assertThat(events.get(1)
                        .get("outcomes")
                        .get(0)
                        .get("initialProbability")
                        .asInt())
                .isEqualTo(34);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"HandDealt", "ProbabilityStateRevealed", "PlayerJammed", "InfluenceTraced", "HandCardIntercepted"
            })
    void targetsViewerScopedEventToPayloadPlayerOnly(String eventType) {
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

        service.fanOut(message(gameId, eventType, "{\"playerId\":\"" + viewerId + "\"}"));

        verify(viewer).send(any());
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

    @Test
    void chainLinkAddedIsBroadcastWithoutThePlayerIdField() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(
                gameId,
                "ChainLinkAdded",
                "{\"gameId\":\"" + gameId + "\",\"playerId\":\"" + playerId + "\",\"chainLength\":2}"));

        var captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(recipient).send(captor.capture());
        assertThat(captor.getValue().payload().has("playerId")).isFalse();
        assertThat(captor.getValue().payload().get("chainLength").asInt()).isEqualTo(2);
    }

    @Test
    void chainBrokenIsBroadcastWithoutTheIdentityField() {
        var gameId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(
                gameId,
                "ChainBroken",
                "{\"gameId\":\"" + gameId + "\",\"playerId\":\"" + UUID.randomUUID() + "\",\"paradoxId\":\""
                        + UUID.randomUUID() + "\",\"chainLengthAtBreak\":3}"));

        var captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(recipient).send(captor.capture());
        assertThat(captor.getValue().payload().has("playerId")).isFalse();
        assertThat(captor.getValue().payload().get("chainLengthAtBreak").asInt())
                .isEqualTo(3);
    }

    @Test
    void chainBrokenAlsoStripsRetiredIdentityFields() {
        var gameId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(
                gameId,
                "ChainBroken",
                "{\"gameId\":\"" + gameId + "\",\"brokenByPlayerId\":\"" + UUID.randomUUID()
                        + "\",\"targetPlayerId\":\"" + UUID.randomUUID() + "\",\"chainLengthAtBreak\":3}"));

        var captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(recipient).send(captor.capture());
        assertThat(captor.getValue().payload().has("brokenByPlayerId")).isFalse();
        assertThat(captor.getValue().payload().has("targetPlayerId")).isFalse();
        assertThat(captor.getValue().payload().get("chainLengthAtBreak").asInt())
                .isEqualTo(3);
    }

    @Test
    void chainLinkInvalidatedIsBroadcastWithoutThePlayerIdField() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var recipient = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session", gameId, UUID.randomUUID(), recipient));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(
                gameId,
                "ChainLinkInvalidated",
                "{\"gameId\":\"" + gameId + "\",\"playerId\":\"" + playerId + "\",\"chainLength\":1}"));

        var captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(recipient).send(captor.capture());
        assertThat(captor.getValue().payload().has("playerId")).isFalse();
        assertThat(captor.getValue().payload().get("chainLength").asInt()).isEqualTo(1);
    }

    @Test
    void scoresUpdatedIsReshapedPerViewer() {
        var gameId = UUID.randomUUID();
        var playerA = UUID.randomUUID();
        var playerB = UUID.randomUUID();
        var sessionA = mock(NotificationDeliveryPort.class);
        var sessionB = mock(NotificationDeliveryPort.class);
        var registry = new NotificationSessionRegistry();
        registry.register(activeSession("session-a", gameId, playerA, sessionA));
        registry.register(activeSession("session-b", gameId, playerB, sessionB));
        var service = new NotificationFanOutService(new NotificationPolicy(), registry, new ObjectMapper());

        service.fanOut(message(gameId, "ScoresUpdated", """
                {"gameId":"%s","eraNumber":1,"updates":[
                    {"playerId":"%s","faction":"PROPHETS","pointsDelta":4,
                     "reason":"EVENT_RESOLVED_AS_WRITTEN","newTotal":12},
                    {"playerId":"%s","faction":"ERASERS","pointsDelta":2,
                     "reason":"ANNIHILATED_OUTCOME","newTotal":6}
                ]}
                """.formatted(gameId, playerA, playerB)));

        var captorA = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(sessionA).send(captorA.capture());
        var updatesForA = captorA.getValue().payload().get("updates");
        assertThat(updatesForA.get(0).has("faction")).isTrue();
        assertThat(updatesForA.get(0).has("reason")).isTrue();
        assertThat(updatesForA.get(1).has("faction")).isFalse();
        assertThat(updatesForA.get(1).has("reason")).isFalse();
        assertThat(updatesForA.get(1).get("playerId").asText()).isEqualTo(playerB.toString());
        assertThat(updatesForA.get(1).get("newTotal").asInt()).isEqualTo(6);

        var captorB = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(sessionB).send(captorB.capture());
        var updatesForB = captorB.getValue().payload().get("updates");
        assertThat(updatesForB.get(0).has("faction")).isFalse();
        assertThat(updatesForB.get(0).has("reason")).isFalse();
        assertThat(updatesForB.get(1).has("faction")).isTrue();
        assertThat(updatesForB.get(1).has("reason")).isTrue();
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
