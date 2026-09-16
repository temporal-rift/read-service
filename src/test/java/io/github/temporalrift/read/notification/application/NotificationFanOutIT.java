package io.github.temporalrift.read.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import io.github.temporalrift.read.ReadServiceIntegrationTest;
import io.github.temporalrift.read.notification.application.port.in.FanOutNotificationUseCase;
import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationRecipient;
import io.github.temporalrift.read.notification.domain.model.NotificationSession;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

@ReadServiceIntegrationTest
class NotificationFanOutIT {

    @Autowired
    FanOutNotificationUseCase fanOut;

    @Autowired
    NotificationSessionRegistry sessions;

    @Test
    void deliversPublicAndTargetedEventsToThreeSessionsWithoutForwardingTheResolutionBarrier() {
        var gameId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var target = register(gameId, targetPlayerId);
        var second = register(gameId, UUID.randomUUID());
        var third = register(gameId, UUID.randomUUID());

        fanOut.fanOut(message(gameId, "ParadoxCascaded", "{}"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(target.messages()).hasSize(1);
            assertThat(second.messages()).hasSize(1);
            assertThat(third.messages()).hasSize(1);
        });

        fanOut.fanOut(message(gameId, "HandDealt", "{\"playerId\":\"" + targetPlayerId + "\"}"));

        assertThat(target.messages()).hasSize(2);
        assertThat(second.messages()).hasSize(1);
        assertThat(third.messages()).hasSize(1);

        fanOut.fanOut(message(gameId, "EraResolutionCompleted", "{}"));

        assertThat(target.messages()).hasSize(2);
        assertThat(second.messages()).hasSize(1);
        assertThat(third.messages()).hasSize(1);
    }

    @Test
    void chainBrokenIsDeliveredToEveryConnectedSessionWithoutIdentityFields() {
        var gameId = UUID.randomUUID();
        var brokenByPlayerId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var sessionOne = register(gameId, UUID.randomUUID());
        var sessionTwo = register(gameId, UUID.randomUUID());

        fanOut.fanOut(message(
                gameId,
                "ChainBroken",
                "{\"gameId\":\"" + gameId + "\",\"brokenByPlayerId\":\"" + brokenByPlayerId + "\",\"targetPlayerId\":\""
                        + targetPlayerId + "\",\"chainLengthAtBreak\":2}"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(sessionOne.messages()).hasSize(1);
            assertThat(sessionTwo.messages()).hasSize(1);
        });
        for (var session : List.of(sessionOne, sessionTwo)) {
            var payload = session.messages().getFirst().payload();
            assertThat(payload.has("brokenByPlayerId")).isFalse();
            assertThat(payload.has("targetPlayerId")).isFalse();
            assertThat(payload.get("chainLengthAtBreak").asInt()).isEqualTo(2);
        }
    }

    @Test
    void scoresUpdatedHidesOpponentFactionAndReasonForMidGameAndEndGameEntries() {
        var gameId = UUID.randomUUID();
        var playerA = UUID.randomUUID();
        var playerB = UUID.randomUUID();
        var playerC = UUID.randomUUID();
        var sessionA = register(gameId, playerA);
        var sessionB = register(gameId, playerB);
        var sessionC = register(gameId, playerC);

        fanOut.fanOut(message(gameId, "ScoresUpdated", """
                {"gameId":"%s","eraNumber":1,"updates":[
                    {"playerId":"%s","faction":"PROPHETS","pointsDelta":4,
                     "reason":"EVENT_RESOLVED_AS_WRITTEN","newTotal":12}
                ]}
                """.formatted(gameId, playerA)));
        fanOut.fanOut(message(gameId, "ScoresUpdated", """
                {"gameId":"%s","eraNumber":0,"updates":[
                    {"playerId":"%s","faction":"REVISIONISTS","pointsDelta":6,
                     "reason":"FACTION_UNIDENTIFIED","newTotal":18}
                ]}
                """.formatted(gameId, playerB)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(sessionA.messages()).hasSize(2);
            assertThat(sessionB.messages()).hasSize(2);
            assertThat(sessionC.messages()).hasSize(2);
        });

        var ownEraOneEntry = sessionA.messages().get(0).payload().get("updates").get(0);
        assertThat(ownEraOneEntry.get("faction").asText()).isEqualTo("PROPHETS");
        assertThat(ownEraOneEntry.get("reason").asText()).isEqualTo("EVENT_RESOLVED_AS_WRITTEN");

        for (var session : List.of(sessionB, sessionC)) {
            var opponentEraOneEntry =
                    session.messages().get(0).payload().get("updates").get(0);
            assertThat(opponentEraOneEntry.has("faction")).isFalse();
            assertThat(opponentEraOneEntry.has("reason")).isFalse();
            assertThat(opponentEraOneEntry.get("playerId").asText()).isEqualTo(playerA.toString());
            assertThat(opponentEraOneEntry.get("newTotal").asInt()).isEqualTo(12);
        }

        var ownEndGameEntry =
                sessionB.messages().get(1).payload().get("updates").get(0);
        assertThat(ownEndGameEntry.get("faction").asText()).isEqualTo("REVISIONISTS");
        assertThat(ownEndGameEntry.get("reason").asText()).isEqualTo("FACTION_UNIDENTIFIED");

        for (var session : List.of(sessionA, sessionC)) {
            var opponentEndGameEntry =
                    session.messages().get(1).payload().get("updates").get(0);
            assertThat(opponentEndGameEntry.has("faction")).isFalse();
            assertThat(opponentEndGameEntry.has("reason")).isFalse();
        }
    }

    private CapturingDelivery register(UUID gameId, UUID playerId) {
        var delivery = new CapturingDelivery();
        var session = new NotificationSession(
                UUID.randomUUID().toString(), new NotificationRecipient(gameId, playerId), delivery, 256);
        session.activate(new NotificationMessage("SNAPSHOT", null, null, null));
        delivery.messages.clear();
        sessions.register(session);
        return delivery;
    }

    private static Message<Object> message(UUID gameId, String eventType, String payload) {
        return MessageBuilder.withPayload((Object) payload.getBytes())
                .setHeader("gameId", gameId.toString())
                .setHeader("eventType", eventType)
                .build();
    }

    private static final class CapturingDelivery implements NotificationDeliveryPort {
        private final List<NotificationMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public void send(NotificationMessage message) {
            messages.add(message);
        }

        List<NotificationMessage> messages() {
            return messages;
        }
    }
}
