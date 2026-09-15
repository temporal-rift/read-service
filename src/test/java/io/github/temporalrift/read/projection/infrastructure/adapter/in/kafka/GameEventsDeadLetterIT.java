package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import io.github.temporalrift.read.ReadServiceIntegrationTest;

/**
 * Proves a poison {@code game.events} record is retried, parked on {@code game.events.dlq} with its original
 * key and identity headers, and does not block a later, unrelated record from still being claimed.
 */
@ReadServiceIntegrationTest
class GameEventsDeadLetterIT {

    private static final String DEAD_LETTER_TOPIC = "game.events.dlq";
    private static final String GAME_EVENTS_TOPIC = "game.events";

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    ConsumerFactory<Object, Object> consumerFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void poisonRecord_isParkedAndFollowingRecordStillProcesses() {
        var gameId = UUID.randomUUID();
        var poisonEventId = UUID.randomUUID();
        var followingEventId = UUID.randomUUID();

        try (var deadLetterConsumer = consumerFactory.createConsumer("dead-letter-it-" + UUID.randomUUID(), "")) {
            deadLetterConsumer.subscribe(List.of(DEAD_LETTER_TOPIC));

            publishPoisonGameStarted(gameId, poisonEventId);
            publishPlainRecord(gameId, followingEventId);

            var parked = await().atMost(Duration.ofSeconds(30))
                    .until(() -> parkedRecord(deadLetterConsumer, poisonEventId), Objects::nonNull);

            assertThat(parked.key()).isEqualTo(gameId.toString());
            assertThat(headerValue(parked, "eventId")).isEqualTo(poisonEventId.toString());
            assertThat(headerValue(parked, "eventType")).isEqualTo("GameStarted");
            assertThat(headerValue(parked, "version")).isEqualTo("1");
            assertThat(headerValue(parked, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).isNotBlank();
            awaitClaim(followingEventId, "projection.game-events");
        }
    }

    private void publishPoisonGameStarted(UUID gameId, UUID eventId) {
        Message<Object> message = MessageBuilder.withPayload((Object) "not-valid-json".getBytes(StandardCharsets.UTF_8))
                .setHeader(KafkaHeaders.TOPIC, GAME_EVENTS_TOPIC)
                .setHeader(KafkaHeaders.KEY, gameId.toString())
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "GameStarted")
                .setHeader("version", "1")
                .build();
        kafkaTemplate.send(message);
    }

    private void publishPlainRecord(UUID gameId, UUID eventId) {
        // Same key as the poison record so both land on the same partition -- proving that partition
        // keeps flowing after the poison record is parked, not just that some other partition is fine.
        Message<Object> message = MessageBuilder.withPayload((Object) new byte[0])
                .setHeader(KafkaHeaders.TOPIC, GAME_EVENTS_TOPIC)
                .setHeader(KafkaHeaders.KEY, gameId.toString())
                .setHeader("eventId", eventId.toString())
                .build();
        kafkaTemplate.send(message);
    }

    private void awaitClaim(UUID eventId, String consumer) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND consumer = ?",
                                Integer.class,
                                eventId,
                                consumer))
                        .isEqualTo(1));
    }

    private static ConsumerRecord<Object, Object> parkedRecord(Consumer<Object, Object> consumer, UUID eventId) {
        var records = consumer.poll(Duration.ofMillis(500));
        return StreamSupport.stream(records.spliterator(), false)
                .filter(consumerRecord -> eventId.toString().equals(headerValue(consumerRecord, "eventId")))
                .findFirst()
                .orElse(null);
    }

    private static String headerValue(ConsumerRecord<?, ?> consumerRecord, String name) {
        var header = consumerRecord.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
