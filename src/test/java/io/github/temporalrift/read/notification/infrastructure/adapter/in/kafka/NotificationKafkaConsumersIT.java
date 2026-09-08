package io.github.temporalrift.read.notification.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.TestcontainersConfiguration;
import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationRecipient;
import io.github.temporalrift.read.notification.domain.model.NotificationSession;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class NotificationKafkaConsumersIT {

    private static final JsonKafkaHeaderMapper HEADER_MAPPER = new JsonKafkaHeaderMapper();

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    NotificationSessionRegistry sessions;

    @Test
    void gameEventsMessage_isClaimedByNotificationGameEventsConsumer() {
        var eventId = UUID.randomUUID();

        publish("game.events", eventId);

        awaitClaim(eventId, "notification.game-events");
    }

    @Test
    void timelineEventsMessage_isClaimedByNotificationTimelineEventsConsumer() {
        var eventId = UUID.randomUUID();

        publish("timeline.events", eventId);

        awaitClaim(eventId, "notification.timeline-events");
    }

    @Test
    void probabilityStateRevealed_publishedWireRecordReachesOnlyTheViewer() {
        var gameId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var viewer = register(gameId, viewerId);
        var otherOne = register(gameId, UUID.randomUUID());
        var otherTwo = register(gameId, UUID.randomUUID());
        var eventId = UUID.randomUUID();

        publish(
                "timeline.events",
                gameId,
                eventId,
                "ProbabilityStateRevealed",
                new ProbabilityStateRevealedPayload(gameId, 1, 1, viewerId, UUID.randomUUID(), List.of()));

        awaitClaim(eventId, "notification.timeline-events");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(viewer.messages()).hasSize(1);
            assertThat(otherOne.messages()).isEmpty();
            assertThat(otherTwo.messages()).isEmpty();
        });
    }

    private void publish(String topic, UUID eventId) {
        publish(topic, UUID.randomUUID(), eventId, "Ignored", Map.of());
    }

    private void publish(String topic, UUID gameId, UUID eventId, String eventType, Object payload) {
        Message<Object> event = MessageBuilder.withPayload((Object) objectMapper.writeValueAsBytes(payload))
                .setHeader("eventId", eventId.toString())
                .setHeader("aggregateId", eventId.toString())
                .setHeader("aggregateType", "FutureEvent")
                .setHeader("gameId", gameId.toString())
                .setHeader("occurredAt", Instant.now().toString())
                .setHeader("version", "1")
                .setHeader("eventType", eventType)
                .build();
        var record = new ProducerRecord<Object, Object>(topic, null, gameId.toString(), event.getPayload());
        HEADER_MAPPER.fromHeaders(event.getHeaders(), record.headers());
        kafkaTemplate.send(record);
    }

    private void awaitClaim(UUID eventId, String consumer) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(countClaims(eventId, consumer)).isEqualTo(1));
    }

    private int countClaims(UUID eventId, String consumer) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND consumer = ?",
                Integer.class,
                eventId,
                consumer);
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
