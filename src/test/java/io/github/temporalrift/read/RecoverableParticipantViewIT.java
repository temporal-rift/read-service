package io.github.temporalrift.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.PlayerPrincipal;
import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

/**
 * End-to-end proof of issue #92: publishes owner facts on {@code game.events}/{@code timeline.events}
 * and asserts {@code GET /state} recovers the participant view after reload — public bands with
 * correction, declarations, Expose facts, own submissions, deadlines, revision and terminal results —
 * while stale facts cannot regress state and opponents learn nothing private.
 */
@ReadServiceIntegrationTest
class RecoverableParticipantViewIT {

    private static final String GAME_EVENTS_TOPIC = "game.events";
    private static final String TIMELINE_EVENTS_TOPIC = "timeline.events";
    private static final JsonKafkaHeaderMapper HEADER_MAPPER = new JsonKafkaHeaderMapper();

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void bandPreview_recoveredThenReplacedByCorrectionAndStaleRedeliveryIgnored() throws Exception {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        startGame(gameId, playerId);

        var previewId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "BandedProbabilityPublished",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                1,
                                "eventStates",
                                List.of(Map.of(
                                        "eventId",
                                        eventId,
                                        "outcomes",
                                        List.of(Map.of("outcomeId", outcomeId, "band", "MEDIUM"))))),
                        previewId)
                .join();
        awaitProcessed(previewId, "projection.game-events");
        awaitBandCount(gameId, 1, 1);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicBands", hasSize(1)))
                .andExpect(jsonPath("$.publicBands[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.publicBands[0].observedInRound").value(2))
                .andExpect(jsonPath("$.publicBands[0].outcomes[0].band").value("MEDIUM"));

        var correctionId = UUID.randomUUID();
        publish(
                        TIMELINE_EVENTS_TOPIC,
                        "AdjustedBandsPublished",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                1,
                                "eventStates",
                                List.of(Map.of(
                                        "eventId",
                                        eventId,
                                        "outcomes",
                                        List.of(Map.of("outcomeId", outcomeId, "band", "HIGH"))))),
                        correctionId)
                .join();
        awaitProcessed(correctionId, "projection.timeline-events");
        awaitBandOutcome(gameId, eventId, outcomeId, "HIGH");

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicBands", hasSize(1)))
                .andExpect(jsonPath("$.publicBands[0].outcomes[0].band").value("HIGH"));

        // Ending the era clears its bands; a redelivered preview for the ended era
        // must not repopulate them.
        var endedId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "EraEnded",
                        gameId,
                        Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2),
                        endedId)
                .join();
        awaitProcessed(endedId, "projection.game-events");
        awaitBandCount(gameId, 1, 0);

        var redeliveryId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "BandedProbabilityPublished",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                1,
                                "eventStates",
                                List.of(Map.of(
                                        "eventId",
                                        eventId,
                                        "outcomes",
                                        List.of(Map.of("outcomeId", outcomeId, "band", "LOW"))))),
                        redeliveryId)
                .join();
        awaitProcessed(redeliveryId, "projection.game-events");
        awaitBandCount(gameId, 1, 0);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicBands").doesNotExist());
    }

    @Test
    void declarationsAndExposeFacts_visibleToEveryParticipant() throws Exception {
        var gameId = UUID.randomUUID();
        var activist = UUID.randomUUID();
        var other = UUID.randomUUID();
        startGame(gameId, activist, other);
        var targetEventId = UUID.randomUUID();
        var targetOutcomeId = UUID.randomUUID();
        var sourceOutcomeId = UUID.randomUUID();

        var declarationId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "ActivistDeclarationRecorded",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                1,
                                "roundNumber",
                                1,
                                "playerId",
                                activist,
                                "mode",
                                "RALLY",
                                "targetEventId",
                                targetEventId,
                                "targetOutcomeId",
                                targetOutcomeId),
                        declarationId)
                .join();
        awaitProcessed(declarationId, "projection.game-events");

        var exposeId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "ExposeSignatureRevealed",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                1,
                                "roundNumber",
                                2,
                                "activistPlayerId",
                                activist,
                                "targetPlayerId",
                                other,
                                "signature",
                                Map.of(
                                        "type",
                                        "SWING",
                                        "targetEventId",
                                        targetEventId,
                                        "sourceOutcomeId",
                                        sourceOutcomeId,
                                        "targetOutcomeId",
                                        targetOutcomeId)),
                        exposeId)
                .join();
        awaitProcessed(exposeId, "projection.game-events");

        for (var viewer : List.of(activist, other)) {
            mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                            .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(viewer)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.declarations", hasSize(1)))
                    .andExpect(jsonPath("$.declarations[0].playerId").value(activist.toString()))
                    .andExpect(jsonPath("$.declarations[0].mode").value("RALLY"))
                    .andExpect(jsonPath("$.exposeFacts", hasSize(1)))
                    .andExpect(jsonPath("$.exposeFacts[0].roundNumber").value(2))
                    .andExpect(jsonPath("$.exposeFacts[0].signature.type").value("SWING"))
                    .andExpect(jsonPath("$.exposeFacts[0].behaviorChanged").value(false));
        }

        // The declarer's own decision is recoverable as their submission; the other player has none.
        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(activist)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySubmissions", hasSize(1)))
                .andExpect(jsonPath("$.mySubmissions[0].kind").value("DECLARATION"));
        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(other)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySubmissions").doesNotExist());
    }

    @Test
    void ownCardPlay_recoveredOnlyByCallerWithRoundAndDeadline() throws Exception {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var opponent = UUID.randomUUID();
        startGame(gameId, playerId, opponent);

        var roundId = UUID.randomUUID();
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
                                List.of(playerId, opponent)),
                        roundId)
                .join();
        awaitProcessed(roundId, "projection.game-events");

        var cardId = UUID.randomUUID();
        var playedId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "CardPlayed",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                1,
                                "roundNumber",
                                1,
                                "playerId",
                                playerId,
                                "cardInstanceId",
                                cardId,
                                "cardType",
                                "PUSH",
                                "grade",
                                "I"),
                        playedId)
                .join();
        awaitProcessed(playedId, "projection.game-events");
        awaitSubmissionCount(gameId, playerId, 1, 1);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.deadlines.actionRoundExpiresAt").exists())
                .andExpect(jsonPath("$.phaseContext.declarationOpen").value(false))
                .andExpect(jsonPath("$.phaseContext.paradoxOpen").value(false))
                .andExpect(jsonPath("$.revision").isNumber())
                .andExpect(jsonPath("$.mySubmissions", hasSize(1)))
                .andExpect(jsonPath("$.mySubmissions[0].kind").value("ACTION"))
                .andExpect(jsonPath("$.mySubmissions[0].actionType").value("CARD"))
                .andExpect(jsonPath("$.mySubmissions[0].status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(opponent)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySubmissions").doesNotExist());
    }

    @Test
    void terminalResult_presentOnlyAfterGameEnded() throws Exception {
        var gameId = UUID.randomUUID();
        var winner = UUID.randomUUID();
        var loser = UUID.randomUUID();
        startGame(gameId, winner, loser);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(winner)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").doesNotExist());

        var winId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "WinConditionMet",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "winnerId",
                                winner,
                                "faction",
                                "WEAVERS",
                                "finalScore",
                                20,
                                "winType",
                                "SCORE_THRESHOLD"),
                        winId)
                .join();
        awaitProcessed(winId, "projection.game-events");

        var endedId = UUID.randomUUID();
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
                                        Map.of("playerId", winner, "faction", "WEAVERS", "score", 20),
                                        Map.of("playerId", loser, "faction", "PROPHETS", "score", 12))),
                        endedId)
                .join();
        awaitProcessed(endedId, "projection.game-events");
        awaitPhase(gameId, "GAME_ENDED");

        for (var viewer : List.of(winner, loser)) {
            mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                            .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(viewer)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phase").value("GAME_ENDED"))
                    .andExpect(jsonPath("$.result.endReason").value("SCORE_THRESHOLD"))
                    .andExpect(jsonPath("$.result.winners[0].playerId").value(winner.toString()))
                    .andExpect(jsonPath("$.result.finalScores", hasSize(2)))
                    .andExpect(jsonPath("$.result.revealBoundary").value("FACTIONS_AND_SCORES_PUBLIC"));
        }
    }

    private void startGame(UUID gameId, UUID... playerIds) throws Exception {
        var startedId = UUID.randomUUID();
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
                                List.of(playerIds),
                                "totalFactions",
                                3,
                                "deckSize",
                                30),
                        startedId)
                .join();
        awaitProcessed(startedId, "projection.game-events");
        awaitPlayerGameStateRowExists(gameId, playerIds[0]);

        var eraId = UUID.randomUUID();
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
                                List.of(playerIds)),
                        eraId)
                .join();
        awaitProcessed(eraId, "projection.game-events");
        awaitPhase(gameId, "ERA_START");
    }

    private CompletableFuture<SendResult<Object, Object>> publish(
            String topic, String eventType, UUID gameId, Object payload) {
        return publish(topic, eventType, gameId, payload, UUID.randomUUID());
    }

    private CompletableFuture<SendResult<Object, Object>> publish(
            String topic, String eventType, UUID gameId, Object payload, UUID eventId) {
        Message<Object> event = MessageBuilder.withPayload((Object) objectMapper.writeValueAsBytes(payload))
                .setHeader("eventId", eventId.toString())
                .setHeader("aggregateId", gameId.toString())
                .setHeader("aggregateType", "Game")
                .setHeader("gameId", gameId.toString())
                .setHeader("occurredAt", Instant.now().toString())
                .setHeader("version", "1")
                .setHeader("eventType", eventType)
                .build();
        var producerRecord = new ProducerRecord<Object, Object>(topic, null, gameId.toString(), event.getPayload());
        HEADER_MAPPER.fromHeaders(event.getHeaders(), producerRecord.headers());
        return kafkaTemplate.send(producerRecord);
    }

    private void awaitProcessed(UUID eventId, String consumer) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM processed_events WHERE event_id = ? AND consumer = ?",
                                Integer.class,
                                eventId,
                                consumer))
                        .isEqualTo(1));
    }

    private void awaitPlayerGameStateRowExists(UUID gameId, UUID playerId) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM player_game_state WHERE game_id = ? AND player_id = ?",
                                Integer.class,
                                gameId,
                                playerId))
                        .isPositive());
    }

    private void awaitPhase(UUID gameId, String phase) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT phase FROM game_projection WHERE game_id = ?", String.class, gameId))
                        .isEqualTo(phase));
    }

    private void awaitBandCount(UUID gameId, int eraNumber, int count) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_public_band WHERE game_id = ? AND era_number = ?",
                                Integer.class,
                                gameId,
                                eraNumber))
                        .isEqualTo(count));
    }

    private void awaitBandOutcome(UUID gameId, UUID eventId, UUID outcomeId, String band) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT outcomes FROM game_public_band WHERE game_id = ? AND event_id = ?",
                                String.class,
                                gameId,
                                eventId))
                        // jsonb text form separates keys from values with a space.
                        .contains("\"band\": \"" + band + "\""));
    }

    private void awaitSubmissionCount(UUID gameId, UUID playerId, int eraNumber, int count) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM player_submission"
                                        + " WHERE game_id = ? AND player_id = ? AND era_number = ?",
                                Integer.class,
                                gameId,
                                playerId,
                                eraNumber))
                        .isEqualTo(count));
    }
}
