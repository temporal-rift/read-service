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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
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
                "Sessionpublish-game-started-out",
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
                "Sessionpublish-faction-assigned-out",
                gameId,
                Map.of("gameId", gameId, "playerId", player1, "faction", "ERASERS"));
        awaitMyFaction(gameId, player1, "ERASERS");

        // Before FactionRevealed: player2's own view shows their faction unset; player1's is never visible to
        // player2 in the shared `players[]` list.
        mockMvc.perform(get("/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player2)))))
                .andExpect(status().isOk())
                // faction is omitted (default-property-inclusion: non_null), not present-with-null, when unset.
                .andExpect(jsonPath("$.players[?(@.playerId=='" + player1 + "')].faction")
                        .isEmpty());

        publish(
                GAME_EVENTS_TOPIC,
                "Sessionpublish-era-started-out",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "cascadedEventIds",
                        List.of(),
                        "playerIds",
                        List.of(player1, player2)));

        publish(
                GAME_EVENTS_TOPIC,
                "Sessionpublish-events-drawn-out",
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
                                "isCascaded",
                                false,
                                "outcomes",
                                List.of(Map.of(
                                        "outcomeId", outcomeId, "description", "d", "initialProbability", 100))))));

        publish(
                GAME_EVENTS_TOPIC,
                "Sessionpublish-hand-dealt-out",
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
                "Actionpublish-action-round-started-out",
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
                "Actionpublish-card-played-out",
                gameId,
                cardPlayedPayload(gameId, player1, cardInstanceId, eventId, outcomeId));
        awaitMyHandSize(gameId, player1, 0);

        publish(
                GAME_EVENTS_TOPIC,
                "Sessionpublish-resolution-started-out",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1));
        awaitPhase(gameId, "RESOLUTION");

        publish(
                TIMELINE_EVENTS_TOPIC,
                "Timelinepublish-outcome-applied-out",
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
                "Scoringpublish-scores-updated-out",
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
                "Sessionpublish-era-ended-out",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2));
        awaitPhase(gameId, "ERA_END");

        publish(
                GAME_EVENTS_TOPIC,
                "Sessionpublish-faction-revealed-out",
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
                "Sessionpublish-game-ended-out",
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

        mockMvc.perform(get("/games/{gameId}/state", gameId)
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
    void nonParticipant_getState_returns404() throws Exception {
        var gameId = UUID.randomUUID();
        var participant = UUID.randomUUID();
        var stranger = UUID.randomUUID();

        publish(
                GAME_EVENTS_TOPIC,
                "Sessionpublish-game-started-out",
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

        mockMvc.perform(get("/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(stranger)))))
                .andExpect(status().isNotFound());
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
        payload.put("targetEventId", targetEventId);
        payload.put("sourceOutcomeId", null);
        payload.put("targetOutcomeId", targetOutcomeId);
        return payload;
    }

    private void publish(String topic, String bindingName, UUID gameId, Object payload) {
        // Producer value-serializer is ByteArraySerializer (pairs with the consumer-side
        // ByteArrayDeserializer), so the payload must already be JSON bytes, not a raw Map.
        Message<Object> message = MessageBuilder.withPayload((Object) objectMapper.writeValueAsBytes(payload))
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("eventId", UUID.randomUUID().toString())
                .setHeader("spring.cloud.stream.sendto.destination", bindingName)
                .build();
        kafkaTemplate.send(message);
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
