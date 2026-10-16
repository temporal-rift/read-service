package io.github.temporalrift.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedOutcomeState;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.shared.PlayerPrincipal;
import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

/**
 * The same ordered records, delivered through Kafka to the production configuration and through
 * {@code simulationEventDelivery} to the {@code in-memory} profile, must leave every seat reading an identical
 * game state after each single fact.
 */
@ReadServiceIntegrationTest
class InMemoryEquivalenceIT {

    private static final String GAME_EVENTS = "game.events";
    private static final String TIMELINE_EVENTS = "timeline.events";
    private static final JsonKafkaHeaderMapper HEADER_MAPPER = new JsonKafkaHeaderMapper();

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc productionMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @SuppressWarnings("unchecked")
    void everySeatReadsTheSameStateAfterEveryRecord() throws Exception {
        var gameId = UUID.randomUUID();
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        var records = scenario(gameId, player1, player2);

        try (var host = new AnnotationConfigApplicationContext();
                var inMemory = startInMemory(host)) {
            var delivery = (Consumer<Map<String, Object>>) inMemory.getBean("simulationEventDelivery");
            var inMemoryMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) inMemory)
                    .apply(springSecurity())
                    .build();

            for (var fact : records) {
                publishToKafka(fact);
                delivery.accept(fact.asDeliveredRecord(objectMapper));
                if (fact.awaitsClaim()) {
                    awaitClaim(fact);
                }
                for (var seat : List.of(player1, player2)) {
                    assertThat(state(inMemoryMvc, gameId, seat))
                            .as("state of %s after %s", seat, fact.eventType())
                            .isEqualTo(state(productionMvc, gameId, seat));
                }
                assertThat(state(inMemoryMvc, gameId, stranger)).isEqualTo(state(productionMvc, gameId, stranger));
            }
        }
    }

    private String state(MockMvc mvc, UUID gameId, UUID seat) throws Exception {
        var response = mvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(seat)))))
                .andReturn()
                .getResponse();
        // lastUpdatedAt is the wall-clock time the projection was written, so it differs between any two runs.
        return response.getStatus() + " "
                + response.getContentAsString().replaceAll("\"lastUpdatedAt\":\"[^\"]*\"", "");
    }

    private static ConfigurableApplicationContext startInMemory(AnnotationConfigApplicationContext host) {
        host.refresh();
        return new SpringApplicationBuilder(ReadServiceApplication.class)
                .parent(host)
                .profiles("in-memory")
                .run(
                        "--server.port=0",
                        "--game.rules.max-eras=5",
                        "--spring.security.oauth2.resourceserver.jwt.issuer-uri=https://host-issuer.invalid",
                        "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://host-issuer.invalid/jwks");
    }

    private void publishToKafka(Fact fact) {
        var producerRecord = new ProducerRecord<Object, Object>(
                fact.topic(), null, fact.gameId().toString(), fact.payload(objectMapper));
        HEADER_MAPPER.fromHeaders(
                new MessageHeaders(new HashMap<String, Object>(fact.headers())), producerRecord.headers());
        kafkaTemplate.send(producerRecord).join();
    }

    private void awaitClaim(Fact fact) {
        var consumer = GAME_EVENTS.equals(fact.topic()) ? "projection.game-events" : "projection.timeline-events";
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND consumer = ?",
                                Integer.class,
                                fact.eventId(),
                                consumer))
                        .isEqualTo(1));
    }

    private static List<Fact> scenario(UUID gameId, UUID player1, UUID player2) {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var cards = new ArrayList<UUID>();
        for (var i = 0; i < 5; i++) {
            cards.add(UUID.randomUUID());
        }
        var records = new ArrayList<Fact>();
        // Timeline facts can arrive before the game exists on their independent consumer group.
        records.add(Fact.of(
                TIMELINE_EVENTS,
                "ProbabilityStateRevealed",
                gameId,
                new ProbabilityStateRevealedPayload(
                        gameId,
                        1,
                        1,
                        player1,
                        eventId,
                        List.of(new ProbabilityStateRevealedOutcomeState(outcomeId, 40, false, false)))));
        records.add(Fact.of(
                GAME_EVENTS,
                "GameStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "winScoreThreshold",
                        20,
                        "lobbyId",
                        UUID.randomUUID(),
                        "players",
                        List.of(
                                Map.of("playerId", player1, "playerName", "Player 1"),
                                Map.of("playerId", player2, "playerName", "Player 2")),
                        "totalFactions",
                        3,
                        "deckSize",
                        30)));
        records.add(Fact.of(
                GAME_EVENTS,
                "FactionAssigned",
                gameId,
                Map.of("gameId", gameId, "playerId", player1, "faction", "ERASERS")));
        records.add(Fact.of(
                GAME_EVENTS,
                "EraStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "carryOverEventIds",
                        List.of(),
                        "playerIds",
                        List.of(player1, player2))));
        records.add(Fact.of(
                GAME_EVENTS,
                "EventsDrawn",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "events",
                        List.of(Map.of(
                                "eventId",
                                eventId,
                                "title",
                                "Test Event",
                                "carryOverState",
                                "FRESH",
                                "outcomes",
                                List.of(Map.of(
                                        "outcomeId", outcomeId, "description", "d", "initialProbability", 100)))))));
        var dealt = List.of(
                dealtCard(cards.get(0), "PUSH", "II", 1),
                dealtCard(cards.get(1), "SCAN", "I", 2),
                dealtCard(cards.get(2), "TRACE", "I", 3),
                dealtCard(cards.get(3), "SWING", "III", 4),
                dealtCard(cards.get(4), "JAM", "II", 5));
        var handDealt = new ArrayList<>(dealt);
        handDealt.add(dealtCard(UUID.randomUUID(), "DECOY", "I", 6));
        handDealt.add(dealtCard(UUID.randomUUID(), "SUPPRESS", "II", 7));
        records.add(Fact.of(
                GAME_EVENTS,
                "HandDealt",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "playerId",
                        player1,
                        "selectionExpiresAt",
                        "2026-08-15T00:00:00Z",
                        "cards",
                        handDealt)));
        records.add(Fact.of(
                GAME_EVENTS,
                "HandSelected",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "playerId",
                        player1,
                        "selectionOrigin",
                        "PLAYER",
                        "cards",
                        dealt)));
        records.add(Fact.of(
                GAME_EVENTS,
                "ActionRoundStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "roundNumber",
                        1,
                        "timerSeconds",
                        60,
                        "pendingPlayerIds",
                        List.of(player1, player2))));
        var played = new HashMap<String, Object>();
        played.put("gameId", gameId);
        played.put("eraNumber", 1);
        played.put("roundNumber", 1);
        played.put("playerId", player1);
        played.put("cardInstanceId", cards.get(0));
        played.put("cardType", "PUSH");
        played.put("grade", "II");
        played.put("targetEventId", eventId);
        played.put("sourceOutcomeId", null);
        played.put("targetOutcomeId", outcomeId);
        var cardPlayed = Fact.of(GAME_EVENTS, "CardPlayed", gameId, played);
        records.add(cardPlayed);
        records.add(cardPlayed.redelivered());
        records.add(Fact.of(GAME_EVENTS, "ResolutionStarted", gameId, Map.of("gameId", gameId, "eraNumber", 1)));
        records.add(Fact.of(
                TIMELINE_EVENTS,
                "OutcomeApplied",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "eventId",
                        eventId,
                        "winningOutcomeId",
                        outcomeId,
                        "finalProbabilities",
                        List.of())));
        records.add(Fact.of(
                GAME_EVENTS,
                "ScoresUpdated",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "updates",
                        List.of(Map.of(
                                "playerId",
                                player1,
                                "faction",
                                "ERASERS",
                                "pointsDelta",
                                4,
                                "reason",
                                "R",
                                "newTotal",
                                4)))));
        records.add(Fact.of(
                GAME_EVENTS,
                "EraEnded",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2)));
        records.add(Fact.of(
                GAME_EVENTS,
                "FactionRevealed",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "reveals",
                        List.of(
                                Map.of("playerId", player1, "faction", "ERASERS"),
                                Map.of("playerId", player2, "faction", "WEAVERS")))));
        records.add(Fact.of(
                GAME_EVENTS,
                "GameEnded",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "endReason",
                        "WIN_CONDITION_MET",
                        "finalScores",
                        List.of(
                                Map.of("playerId", player1, "faction", "ERASERS", "score", 4),
                                Map.of("playerId", player2, "faction", "WEAVERS", "score", 0)))));
        return records;
    }

    private static Map<String, Object> dealtCard(UUID cardInstanceId, String cardType, String grade, int dealSlot) {
        return Map.of("cardInstanceId", cardInstanceId, "cardType", cardType, "grade", grade, "dealSlot", dealSlot);
    }

    private record Fact(String topic, String eventType, UUID gameId, UUID eventId, Object body, boolean redelivery) {

        static Fact of(String topic, String eventType, UUID gameId, Object body) {
            return new Fact(topic, eventType, gameId, UUID.randomUUID(), body, false);
        }

        Fact redelivered() {
            return new Fact(topic, eventType, gameId, eventId, body, true);
        }

        boolean awaitsClaim() {
            return !redelivery;
        }

        byte[] payload(ObjectMapper objectMapper) {
            return objectMapper.writeValueAsBytes(body);
        }

        Map<String, String> headers() {
            return Map.of(
                    "eventId", eventId.toString(),
                    "aggregateId", gameId.toString(),
                    "aggregateType", "Game",
                    "gameId", gameId.toString(),
                    "occurredAt", OCCURRED_AT.toString(),
                    "version", "1",
                    "eventType", eventType);
        }

        Map<String, Object> asDeliveredRecord(ObjectMapper objectMapper) {
            return Map.of(
                    "topic", topic, "key", gameId.toString(), "headers", headers(), "payload", payload(objectMapper));
        }

        private static final Instant OCCURRED_AT = Instant.parse("2026-08-01T00:00:00Z");
    }
}
