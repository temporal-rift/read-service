package io.github.temporalrift.read.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import io.github.temporalrift.read.TestcontainersConfiguration;
import io.github.temporalrift.read.notification.application.port.in.FanOutNotificationUseCase;
import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationRecipient;
import io.github.temporalrift.read.notification.domain.model.NotificationSession;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
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

    private CapturingDelivery register(UUID gameId, UUID playerId) {
        var delivery = new CapturingDelivery();
        var session = new NotificationSession(
                UUID.randomUUID().toString(), new NotificationRecipient(gameId, playerId), delivery);
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
