package io.github.temporalrift.read;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.PlayerPrincipal;
import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

/**
 * End-to-end proof of issue #8: publishes a realistic event sequence on {@code game.events}/{@code
 * timeline.events} and asserts {@code GET /state} reflects it — the spec's "rebuild from events", "phase
 * advances", "own faction private / other factions hidden until reveal", and "non-participant is rejected"
 * scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class PlayerGameStateIT {

    private static final String GAME_EVENTS_TOPIC = "game.events";
    private static final String TIMELINE_EVENTS_TOPIC = "timeline.events";

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fullEventSequence_reflectsInGetState_andHidesFactionUntilRevealed() throws Exception {
        var gameId = UUID.randomUUID();
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();

        publish(
                GAME_EVENTS_TOPIC,
                "GameStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "lobbyId",
                        UUID.randomUUID(),
                        "playerIds",
                        List.of(player1, player2),
                        "totalFactions",
                        3,
                        "deckSize",
                        30));
        awaitPlayerGameStateRowExists(gameId, player1);

        publish(
                GAME_EVENTS_TOPIC,
                "FactionAssigned",
                gameId,
                Map.of("gameId", gameId, "playerId", player1, "faction", "ERASERS"));
        awaitMyFaction(gameId, player1, "ERASERS");

        // Before FactionRevealed: player2's own view shows their faction unset; player1's is never visible to
        // player2 in the shared `players[]` list.
        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player2)))))
                .andExpect(status().isOk())
                // faction is omitted (default-property-inclusion: non_null), not present-with-null, when unset.
                .andExpect(jsonPath("$.players[?(@.playerId=='" + player1 + "')].faction")
                        .isEmpty());

        publish(
                GAME_EVENTS_TOPIC,
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
                        List.of(player1, player2)));

        publish(
                GAME_EVENTS_TOPIC,
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
                                        "outcomeId", outcomeId, "description", "d", "initialProbability", 100))))));

        publish(
                GAME_EVENTS_TOPIC,
                "HandDealt",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "playerId",
                        player1,
                        "cards",
                        List.of(Map.of("cardInstanceId", cardInstanceId, "cardType", "PUSH"))));
        awaitMyHandSize(gameId, player1, 1);

        publish(
                GAME_EVENTS_TOPIC,
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
                        List.of(player1, player2)));
        awaitPhase(gameId, "ACTION_ROUND_1");

        publish(
                GAME_EVENTS_TOPIC,
                "CardPlayed",
                gameId,
                cardPlayedPayload(gameId, player1, cardInstanceId, eventId, outcomeId));
        awaitMyHandSize(gameId, player1, 0);

        publish(GAME_EVENTS_TOPIC, "ResolutionStarted", gameId, Map.of("gameId", gameId, "eraNumber", 1));
        awaitPhase(gameId, "RESOLUTION");

        publish(
                TIMELINE_EVENTS_TOPIC,
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
                        List.of()));
        awaitActiveEventRemoved(gameId, eventId);

        publish(
                GAME_EVENTS_TOPIC,
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
                                4))));
        awaitScore(gameId, player1, 4);

        publish(
                GAME_EVENTS_TOPIC,
                "EraEnded",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2));
        awaitPhase(gameId, "ERA_END");

        publish(
                GAME_EVENTS_TOPIC,
                "FactionRevealed",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "reveals",
                        List.of(
                                Map.of("playerId", player1, "faction", "ERASERS"),
                                Map.of("playerId", player2, "faction", "WEAVERS"))));

        publish(
                GAME_EVENTS_TOPIC,
                "GameEnded",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "endReason",
                        "SCORE_THRESHOLD",
                        "finalScores",
                        List.of(
                                Map.of("playerId", player1, "faction", "ERASERS", "score", 4),
                                Map.of("playerId", player2, "faction", "WEAVERS", "score", 0))));
        awaitPhase(gameId, "GAME_ENDED");

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eraNumber").value(1))
                .andExpect(jsonPath("$.phase").value("GAME_ENDED"))
                .andExpect(jsonPath("$.myFaction").value("ERASERS"))
                .andExpect(jsonPath("$.myHand").isEmpty())
                .andExpect(jsonPath("$.myScore").value(4))
                .andExpect(jsonPath("$.activeEvents").isEmpty())
                .andExpect(jsonPath("$.players[?(@.playerId=='" + player2 + "')].faction")
                        .value(org.hamcrest.Matchers.contains("WEAVERS")));
    }

    @Test
    void paradoxResolutionPhase_opensThenClosesBackToResolution() throws Exception {
        var gameId = UUID.randomUUID();
        var player1 = UUID.randomUUID();
        var paradox1 = UUID.randomUUID();
        var paradox2 = UUID.randomUUID();

        publish(
                GAME_EVENTS_TOPIC,
                "GameStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "lobbyId",
                        UUID.randomUUID(),
                        "playerIds",
                        List.of(player1),
                        "totalFactions",
                        3,
                        "deckSize",
                        30));
        awaitPlayerGameStateRowExists(gameId, player1);

        publish(GAME_EVENTS_TOPIC, "ResolutionStarted", gameId, Map.of("gameId", gameId, "eraNumber", 1));
        awaitPhase(gameId, "RESOLUTION");

        publish(
                TIMELINE_EVENTS_TOPIC,
                "ParadoxResolutionPhaseStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "paradoxIds",
                        List.of(paradox1, paradox2),
                        "timerSeconds",
                        60));
        awaitPhase(gameId, "PARADOX_RESOLUTION");

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("PARADOX_RESOLUTION"));

        publish(
                TIMELINE_EVENTS_TOPIC,
                "ParadoxResolved",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "paradoxId", paradox1, "resolvedByPlayerId", player1));
        awaitPendingParadoxCount(gameId, 1);

        // One of two paradoxes still pending — phase must stay open.
        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("PARADOX_RESOLUTION"));

        publish(
                TIMELINE_EVENTS_TOPIC,
                "ParadoxCascaded",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "paradoxId",
                        paradox2,
                        "affectedEventId",
                        UUID.randomUUID(),
                        "carryForwardProbabilityState",
                        List.of()));
        awaitPhase(gameId, "RESOLUTION");
    }

    @Test
    void nonParticipant_getState_returns404() throws Exception {
        var gameId = UUID.randomUUID();
        var participant = UUID.randomUUID();
        var stranger = UUID.randomUUID();

        publish(
                GAME_EVENTS_TOPIC,
                "GameStarted",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "lobbyId",
                        UUID.randomUUID(),
                        "playerIds",
                        List.of(participant),
                        "totalFactions",
                        3,
                        "deckSize",
                        30));
        awaitPlayerGameStateRowExists(gameId, participant);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(stranger)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void publish_keysTheProducerRecordByGameId() {
        var gameId = UUID.randomUUID();

        var sendResult = publish(GAME_EVENTS_TOPIC, "GameStarted", gameId, Map.of("gameId", gameId))
                .join();

        // Matches production, where game-service's binder config and timeline-service's OutboxRelay both
        // key by gameId so all of one game's events land on the same partition. Without it, this would be
        // null (no key set), not just a different value.
        org.assertj.core.api.Assertions.assertThat(
                        sendResult.getProducerRecord().key())
                .isEqualTo(gameId.toString());
    }

    private static Map<String, Object> cardPlayedPayload(
            UUID gameId, UUID playerId, UUID cardInstanceId, UUID targetEventId, UUID targetOutcomeId) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("gameId", gameId);
        payload.put("eraNumber", 1);
        payload.put("roundNumber", 1);
        payload.put("playerId", playerId);
        payload.put("cardInstanceId", cardInstanceId);
        payload.put("cardType", "PUSH");
        payload.put("grade", "II");
        payload.put("targetEventId", targetEventId);
        payload.put("sourceOutcomeId", null);
        payload.put("targetOutcomeId", targetOutcomeId);
        return payload;
    }

    private CompletableFuture<SendResult<Object, Object>> publish(
            String topic, String eventType, UUID gameId, Object payload) {
        // Producer value-serializer is ByteArraySerializer (pairs with the consumer-side
        // ByteArrayDeserializer), so the payload must already be JSON bytes, not a raw Map.
        // Keyed by gameId to match production, where game-service's binder config and timeline-service's
        // OutboxRelay both do the same -- without a key, Kafka's default partitioner spreads a single
        // game's events across partitions with no ordering guarantee between them.
        Message<Object> message = MessageBuilder.withPayload((Object) objectMapper.writeValueAsBytes(payload))
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, gameId.toString())
                .setHeader("eventId", UUID.randomUUID().toString())
                .setHeader("eventType", eventType)
                .build();
        return kafkaTemplate.send(message);
    }

    private void awaitPlayerGameStateRowExists(UUID gameId, UUID playerId) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM player_game_state WHERE game_id = ? AND player_id = ?",
                                Integer.class,
                                gameId,
                                playerId))
                        .isPositive());
    }

    private void awaitMyFaction(UUID gameId, UUID playerId, String faction) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT my_faction FROM player_game_state WHERE game_id = ? AND player_id = ?",
                                String.class,
                                gameId,
                                playerId))
                        .isEqualTo(faction));
    }

    private void awaitMyHandSize(UUID gameId, UUID playerId, int size) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var stateId = jdbcTemplate.queryForObject(
                    "SELECT id FROM player_game_state WHERE game_id = ? AND player_id = ?",
                    UUID.class,
                    gameId,
                    playerId);
            var count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM player_game_state_hand_card WHERE player_game_state_id = ?",
                    Integer.class,
                    stateId);
            org.assertj.core.api.Assertions.assertThat(count).isEqualTo(size);
        });
    }

    private void awaitPhase(UUID gameId, String phase) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT phase FROM game_projection WHERE game_id = ?", String.class, gameId))
                        .isEqualTo(phase));
    }

    private void awaitPendingParadoxCount(UUID gameId, int count) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_projection_pending_paradox WHERE game_projection_id = ?",
                                Integer.class,
                                gameId))
                        .isEqualTo(count));
    }

    private void awaitActiveEventRemoved(UUID gameId, UUID eventId) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_active_event WHERE game_id = ? AND event_id = ?",
                                Integer.class,
                                gameId,
                                eventId))
                        .isZero());
    }

    private void awaitScore(UUID gameId, UUID playerId, int score) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT score FROM game_player WHERE game_id = ? AND player_id = ?",
                                Integer.class,
                                gameId,
                                playerId))
                        .isEqualTo(score));
    }
}
