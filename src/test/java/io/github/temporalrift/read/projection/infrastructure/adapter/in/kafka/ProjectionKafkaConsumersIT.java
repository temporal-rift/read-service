package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

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

/**
 * Proves both consumer groups actually receive and idempotently claim real Kafka messages — the core acceptance
 * criterion of read-service issue #7 ("Both consumer groups receive test events in an IT").
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProjectionKafkaConsumersIT {

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void gameEventsMessage_isClaimedByProjectionGameEventsConsumer() {
        var eventId = UUID.randomUUID();

        publish("game.events", eventId);

        awaitClaim(eventId, "projection.game-events");
    }

    @Test
    void timelineEventsMessage_isClaimedByProjectionTimelineEventsConsumer() {
        var eventId = UUID.randomUUID();

        publish("timeline.events", eventId);

        awaitClaim(eventId, "projection.timeline-events");
    }

    @Test
    void redeliveredGameEventsMessage_isNotClaimedTwice() {
        var eventId = UUID.randomUUID();

        publish("game.events", eventId);
        awaitClaim(eventId, "projection.game-events");

        publish("game.events", eventId);

        await().pollDelay(Duration.ofSeconds(5))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(countClaims(eventId, "projection.game-events"))
                        .isEqualTo(1));
    }

    private void publish(String topic, UUID eventId) {
        // Only the eventId header — see KafkaTestMessages for why the other envelope headers are omitted.
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
