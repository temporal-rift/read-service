package io.github.temporalrift.read.notification.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import io.github.temporalrift.read.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class NotificationKafkaConsumersIT {

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

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

    private void publish(String topic, UUID eventId) {
        Message<Object> message = MessageBuilder.withPayload((Object) new byte[0])
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("eventId", eventId.toString())
                .build();
        kafkaTemplate.send(message);
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
}
