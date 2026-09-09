package io.github.temporalrift.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedOutcomeState;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
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
    void roundSummaryPublished_replacesTheSharedLastRoundSummaryWithoutExposingPrivateActionDetails() throws Exception {
        var gameId = UUID.randomUUID();
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        var player3 = UUID.randomUUID();
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
        awaitPlayerGameStateRowExists(gameId, player2);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastRoundSummary").value(nullValue()));

        publish(
                GAME_EVENTS_TOPIC,
                "RoundSummaryPublished",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "roundNumber",
                        1,
                        "actionSummaries",
                        List.of(Map.of(
                                "playerId",
                                player1,
                                "actionCategory",
                                "PROBABILITY_SHIFTER",
                                "actionFamily",
                                "CARD",
                                "skipped",
                                false))));
        awaitLastRoundSummary(gameId, 1);
        publish(
                GAME_EVENTS_TOPIC,
                "RoundSummaryPublished",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "roundNumber",
                        2,
                        "actionSummaries",
                        List.of(
                                Map.of(
                                        "playerId",
                                        player2,
                                        "actionCategory",
                                        "INFORMATION",
                                        "actionFamily",
                                        "CARD",
                                        "skipped",
                                        true),
                                Map.of(
                                        "playerId",
                                        player1,
                                        "actionCategory",
                                        "PROBABILITY_SHIFTER",
                                        "actionFamily",
                                        "CARD",
                                        "skipped",
                                        false),
                                // A player who played no card at all: actionCategory/actionFamily are
                                // legitimately absent (not just false-skipped-with-a-category), per the
                                // event contract's ActionSummary schema, which only requires playerId/skipped.
                                Map.of("playerId", player3, "skipped", true))));
        awaitLastRoundSummary(gameId, 2);

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
                        3,
                        "timerSeconds",
                        30,
                        "pendingPlayerIds",
                        List.of(player1, player2)));
        awaitLastRoundSummary(gameId, 2);

        for (var playerId : List.of(player1, player2)) {
            mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                            .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastRoundSummary.roundNumber").value(2))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[0].playerId")
                            .value(player2.toString()))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[0].actionCategory")
                            .value("INFORMATION"))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[0].actionFamily")
                            .value("CARD"))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[0].skipped")
                            .value(true))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[0].cardType")
                            .doesNotExist())
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[0].targetEventId")
                            .doesNotExist())
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[1].playerId")
                            .value(player1.toString()))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[1].actionCategory")
                            .value("PROBABILITY_SHIFTER"))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[2].playerId")
                            .value(player3.toString()))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[2].skipped")
                            .value(true))
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[2].actionCategory")
                            .doesNotExist())
                    .andExpect(jsonPath("$.lastRoundSummary.actionSummaries[2].actionFamily")
                            .doesNotExist());
        }

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT action_category FROM game_projection_last_round_summary_action "
                                + "WHERE game_id = ? AND player_id = ?",
                        String.class,
                        gameId,
                        player3))
                .isNull();
    }

    @Test
    void fullEventSequence_reflectsInGetState_andHidesFactionUntilRevealed() throws Exception {
        var gameId = UUID.randomUUID();
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();
        var selectedCardInstanceIds =
                List.of(cardInstanceId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var pendingCardInstanceId = UUID.randomUUID();
        var expiresAt = "2026-08-15T00:00:00Z";

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
                        "selectionExpiresAt",
                        expiresAt,
                        "cards",
                        List.of(
                                dealtCard(selectedCardInstanceIds.get(0), "PUSH", "II", 1),
                                dealtCard(selectedCardInstanceIds.get(1), "SCAN", "I", 2),
                                dealtCard(selectedCardInstanceIds.get(2), "TRACE", "I", 3),
                                dealtCard(selectedCardInstanceIds.get(3), "SWING", "III", 4),
                                dealtCard(selectedCardInstanceIds.get(4), "JAM", "II", 5),
                                dealtCard(pendingCardInstanceId, "DECOY", "I", 6),
                                dealtCard(UUID.randomUUID(), "SUPPRESS", "II", 7))));
        awaitPendingHandSelectionCardCount(gameId, player1, 7);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myHand").isEmpty())
                .andExpect(jsonPath("$.pendingHandSelection.cards.length()").value(7))
                .andExpect(jsonPath("$.pendingHandSelection.cards[0].cardInstanceId")
                        .value(cardInstanceId.toString()))
                .andExpect(jsonPath("$.pendingHandSelection.cards[0].grade").value("II"))
                .andExpect(jsonPath("$.pendingHandSelection.cards[0].dealSlot").value(1))
                .andExpect(jsonPath("$.pendingHandSelection.requiredSelectionCount")
                        .value(5))
                .andExpect(jsonPath("$.pendingHandSelection.expiresAt").value(expiresAt));

        publish(
                GAME_EVENTS_TOPIC,
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
                        List.of(
                                dealtCard(selectedCardInstanceIds.get(0), "PUSH", "II", 1),
                                dealtCard(selectedCardInstanceIds.get(1), "SCAN", "I", 2),
                                dealtCard(selectedCardInstanceIds.get(2), "TRACE", "I", 3),
                                dealtCard(selectedCardInstanceIds.get(3), "SWING", "III", 4),
                                dealtCard(selectedCardInstanceIds.get(4), "JAM", "II", 5))));
        awaitMyHandSize(gameId, player1, 5);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingHandSelection").doesNotExist())
                .andExpect(jsonPath("$.myHand.length()").value(5))
                .andExpect(jsonPath("$.myHand[0].grade").value("II"));

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

        for (var selectedCardInstanceId : selectedCardInstanceIds) {
            var cardPlayedPayload = selectedCardInstanceId.equals(selectedCardInstanceIds.get(1))
                    ? multiTargetCardPlayedPayload(
                            gameId, player1, selectedCardInstanceId, List.of(eventId, UUID.randomUUID()))
                    : cardPlayedPayload(gameId, player1, selectedCardInstanceId, eventId, outcomeId);
            publish(GAME_EVENTS_TOPIC, "CardPlayed", gameId, cardPlayedPayload);
        }
        awaitMyHandSize(gameId, player1, 0);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(player1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..targetEventIds").doesNotExist());

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
                        .value(contains("WEAVERS")));
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
    void paradoxPhase_arrivingBeforeGameStarted_isAppliedWithoutRetryAndSurvivesGameStarted() throws Exception {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var paradoxId = UUID.randomUUID();
        var paradoxEventId = UUID.randomUUID();

        publish(
                        TIMELINE_EVENTS_TOPIC,
                        "ParadoxResolutionPhaseStarted",
                        gameId,
                        Map.of("gameId", gameId, "eraNumber", 1, "paradoxIds", List.of(paradoxId), "timerSeconds", 60),
                        paradoxEventId)
                .join();
        awaitProcessed(paradoxEventId, "projection.timeline-events");
        awaitEraAndPhase(gameId, 1, "PARADOX_RESOLUTION");
        awaitPendingParadoxCount(gameId, 1);

        var gameStartedEventId = UUID.randomUUID();
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
                                List.of(playerId),
                                "totalFactions",
                                3,
                                "deckSize",
                                30),
                        gameStartedEventId)
                .join();
        awaitProcessed(gameStartedEventId, "projection.game-events");
        awaitPlayerGameStateRowExists(gameId, playerId);
        awaitEraAndPhase(gameId, 1, "PARADOX_RESOLUTION");
        awaitPendingParadoxCount(gameId, 1);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eraNumber").value(1))
                .andExpect(jsonPath("$.phase").value("PARADOX_RESOLUTION"));
    }

    @Test
    void eventsDrawn_forFutureEra_advancesProjectionBeforeExposingEvents() throws Exception {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var activeEventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();

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
                        List.of(playerId),
                        "totalFactions",
                        3,
                        "deckSize",
                        30));
        awaitPlayerGameStateRowExists(gameId, playerId);
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
                        List.of(playerId)));
        awaitEraAndPhase(gameId, 1, "ERA_START");
        publish(
                GAME_EVENTS_TOPIC,
                "EraEnded",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2));
        awaitEraAndPhase(gameId, 1, "ERA_END");

        var eventsDrawnEventId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "EventsDrawn",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                2,
                                "events",
                                List.of(Map.of(
                                        "eventId",
                                        activeEventId,
                                        "title",
                                        "Early next-era event",
                                        "carryOverState",
                                        "FRESH",
                                        "outcomes",
                                        List.of(Map.of(
                                                "outcomeId",
                                                outcomeId,
                                                "description",
                                                "outcome",
                                                "initialProbability",
                                                100))))),
                        eventsDrawnEventId)
                .join();
        awaitProcessed(eventsDrawnEventId, "projection.game-events");
        awaitEraAndPhase(gameId, 2, "ERA_START");
        awaitActiveEventExists(gameId, activeEventId);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eraNumber").value(2))
                .andExpect(jsonPath("$.phase").value("ERA_START"))
                .andExpect(jsonPath("$.activeEvents[0].eventId").value(activeEventId.toString()));

        var delayedEraStartedEventId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "EraStarted",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "eraNumber",
                                2,
                                "carryOverEventIds",
                                List.of(),
                                "playerIds",
                                List.of(playerId)),
                        delayedEraStartedEventId)
                .join();
        awaitProcessed(delayedEraStartedEventId, "projection.game-events");
        awaitEraAndPhase(gameId, 2, "ERA_START");
        awaitActiveEventExists(gameId, activeEventId);
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

        var sendResult = publish(
                        GAME_EVENTS_TOPIC,
                        "GameStarted",
                        gameId,
                        Map.of(
                                "gameId",
                                gameId,
                                "lobbyId",
                                UUID.randomUUID(),
                                "playerIds",
                                List.of(UUID.randomUUID(), UUID.randomUUID()),
                                "totalFactions",
                                3,
                                "deckSize",
                                30))
                .join();

        // Matches production, where game-service's binder config and timeline-service's OutboxRelay both
        // key by gameId so all of one game's events land on the same partition. Without it, this would be
        // null (no key set), not just a different value.
        assertThat(sendResult.getProducerRecord().key()).isEqualTo(gameId.toString());
    }

    @Test
    void probabilityIntel_survivesEarlyDeliveryAndReconnectWithoutLeakingOrCrossingEraBoundaries() throws Exception {
        var gameId = UUID.randomUUID();
        var scanningPlayerId = UUID.randomUUID();
        var otherPlayerId = UUID.randomUUID();
        var scannedEventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var firstReveal = new ProbabilityStateRevealedPayload(
                gameId,
                1,
                1,
                scanningPlayerId,
                scannedEventId,
                List.of(new ProbabilityStateRevealedOutcomeState(outcomeId, 40, false, false)));

        // timeline.events may arrive before the matching game.events state on its independent consumer group.
        publish(TIMELINE_EVENTS_TOPIC, "ProbabilityStateRevealed", gameId, firstReveal);
        awaitProbabilityIntelCount(gameId, scanningPlayerId, 1, 1);

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
                        List.of(scanningPlayerId, otherPlayerId),
                        "totalFactions",
                        3,
                        "deckSize",
                        30));
        awaitPlayerGameStateRowExists(gameId, scanningPlayerId);

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
                        List.of(scanningPlayerId, otherPlayerId)));
        awaitPhase(gameId, "ERA_START");

        // A later Scan result for the same viewer/event replaces the round-one snapshot.
        publish(
                TIMELINE_EVENTS_TOPIC,
                "ProbabilityStateRevealed",
                gameId,
                new ProbabilityStateRevealedPayload(
                        gameId,
                        1,
                        2,
                        scanningPlayerId,
                        scannedEventId,
                        List.of(new ProbabilityStateRevealedOutcomeState(outcomeId, 60, false, true))));
        awaitProbabilityIntelProbability(gameId, scanningPlayerId, 1, scannedEventId, 60);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(scanningPlayerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRevealedIntel.length()").value(1))
                .andExpect(jsonPath("$.myRevealedIntel[0].kind").value("PROBABILITY"))
                .andExpect(jsonPath("$.myRevealedIntel[0].observedInRound").value(2))
                .andExpect(jsonPath("$.myRevealedIntel[0].eventId").value(scannedEventId.toString()))
                .andExpect(
                        jsonPath("$.myRevealedIntel[0].outcomes[0].probability").value(60))
                .andExpect(jsonPath("$.myRevealedIntel[0].outcomes[0].isSealed").value(true));

        // A reconnect/read must see only the caller's persisted, current-era projection.
        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(otherPlayerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRevealedIntel").isEmpty());

        publish(
                GAME_EVENTS_TOPIC,
                "EraEnded",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2));
        awaitPhase(gameId, "ERA_END");
        awaitProbabilityIntelCount(gameId, scanningPlayerId, 1, 0);

        // A late record from the completed era must not restore private intel into the next lifecycle state.
        // Waiting on processed_events for this specific eventId — not a fixed pollDelay — is what makes the
        // "still 0" assertion mean something; otherwise a consumer that just hadn't reached the message yet
        // would pass the same check for the wrong reason.
        var lateRevealEventId = UUID.randomUUID();
        publish(TIMELINE_EVENTS_TOPIC, "ProbabilityStateRevealed", gameId, firstReveal, lateRevealEventId)
                .join();
        awaitProcessed(lateRevealEventId, "projection.timeline-events");
        assertThatProbabilityIntelCount(gameId, scanningPlayerId, 1, 0);

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(scanningPlayerId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRevealedIntel").isEmpty());
    }

    // game.events and timeline.events are independent consumer groups with no ordering guarantee between
    // them, so EraEnded and a same-era ProbabilityStateRevealed genuinely race on separate threads here —
    // this isn't simulated. Repeated across fresh games to actually exercise both interleavings rather than
    // relying on one lucky scheduling.
    @Test
    void probabilityIntel_racingEraEndAndReveal_neverLeavesAnOrphanedRowEitherWay() {
        for (var i = 0; i < 20; i++) {
            var gameId = UUID.randomUUID();
            var playerId = UUID.randomUUID();
            var eventId = UUID.randomUUID();
            var outcomeId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO game_projection (game_id, era_number, phase) VALUES (?, ?, ?)",
                    gameId,
                    1,
                    "ACTION_ROUND_3");
            var reveal = new ProbabilityStateRevealedPayload(
                    gameId,
                    1,
                    3,
                    playerId,
                    eventId,
                    List.of(new ProbabilityStateRevealedOutcomeState(outcomeId, 50, false, false)));

            var eraEndedEventId = UUID.randomUUID();
            var revealEventId = UUID.randomUUID();
            var eraEndedSend = publish(
                    GAME_EVENTS_TOPIC,
                    "EraEnded",
                    gameId,
                    Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2),
                    eraEndedEventId);
            var revealSend = publish(TIMELINE_EVENTS_TOPIC, "ProbabilityStateRevealed", gameId, reveal, revealEventId);
            CompletableFuture.allOf(eraEndedSend, revealSend).join();

            // The race itself already happened for real inside Postgres by the time both sends return —
            // waiting on processed_events for each specific eventId (not a polling window on the outcome)
            // just makes the final check deterministic instead of a timing guess.
            awaitProcessed(eraEndedEventId, "projection.game-events");
            awaitProcessed(revealEventId, "projection.timeline-events");

            assertThat(jdbcTemplate.queryForObject(
                            "SELECT phase FROM game_projection WHERE game_id = ?", String.class, gameId))
                    .isEqualTo("ERA_END");
            assertThatProbabilityIntelCount(gameId, playerId, 1, 0);
        }
    }

    // Real cross-topic delayed delivery, not a mock: EraEnded lands and settles first, then a paradox event
    // for the era it just closed arrives afterward on timeline.events' own consumer thread. isPastOrTerminalEra
    // rejects it for all three paradox entry points identically (unit-tested individually); this proves the
    // wiring end-to-end for one of them. Waiting on processed_events for the paradox event's own eventId,
    // rather than a fixed pollDelay, is what makes the final phase assertion mean something — otherwise a
    // consumer that simply hadn't gotten to the message yet would pass the same assertion for the wrong reason.
    @Test
    void paradoxResolutionPhaseStarted_arrivingAfterEraAlreadyEnded_doesNotReopenTheClosedEra() {
        var gameId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO game_projection (game_id, era_number, phase) VALUES (?, ?, ?)",
                gameId,
                1,
                "ACTION_ROUND_3");

        var eraEndedEventId = UUID.randomUUID();
        publish(
                        GAME_EVENTS_TOPIC,
                        "EraEnded",
                        gameId,
                        Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2),
                        eraEndedEventId)
                .join();
        awaitProcessed(eraEndedEventId, "projection.game-events");
        awaitPhase(gameId, "ERA_END");

        var delayedParadoxEventId = UUID.randomUUID();
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
                                List.of(UUID.randomUUID()),
                                "timerSeconds",
                                60),
                        delayedParadoxEventId)
                .join();
        awaitProcessed(delayedParadoxEventId, "projection.timeline-events");

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT phase FROM game_projection WHERE game_id = ?", String.class, gameId))
                .isEqualTo("ERA_END");
    }

    private static Map<String, Object> cardPlayedPayload(
            UUID gameId, UUID playerId, UUID cardInstanceId, UUID targetEventId, UUID targetOutcomeId) {
        var payload = new HashMap<String, Object>();
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

    private static Map<String, Object> multiTargetCardPlayedPayload(
            UUID gameId, UUID playerId, UUID cardInstanceId, List<UUID> targetEventIds) {
        var payload = new HashMap<String, Object>();
        payload.put("gameId", gameId);
        payload.put("eraNumber", 1);
        payload.put("roundNumber", 1);
        payload.put("playerId", playerId);
        payload.put("cardInstanceId", cardInstanceId);
        payload.put("cardType", "SCAN");
        payload.put("grade", "I");
        payload.put("targetEventIds", targetEventIds);
        return payload;
    }

    private static Map<String, Object> dealtCard(UUID cardInstanceId, String cardType, String grade, int dealSlot) {
        return Map.of("cardInstanceId", cardInstanceId, "cardType", cardType, "grade", grade, "dealSlot", dealSlot);
    }

    private CompletableFuture<SendResult<Object, Object>> publish(
            String topic, String eventType, UUID gameId, Object payload) {
        return publish(topic, eventType, gameId, payload, UUID.randomUUID());
    }

    private CompletableFuture<SendResult<Object, Object>> publish(
            String topic, String eventType, UUID gameId, Object payload, UUID eventId) {
        // Producer value-serializer is ByteArraySerializer (pairs with the consumer-side
        // ByteArrayDeserializer), so the payload must already be JSON bytes, not a raw Map.
        // Keyed by gameId to match production, where game-service's binder config and timeline-service's
        // OutboxRelay both do the same -- without a key, Kafka's default partitioner spreads a single
        // game's events across partitions with no ordering guarantee between them.
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

    // Proves the consumer actually finished handling this specific message, not just that KafkaTemplate
    // acked the send or that a fixed delay has elapsed — either of those lets a not-yet-processed message
    // pass an assertion that happens to already be true for an unrelated reason.
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

    private void awaitMyFaction(UUID gameId, UUID playerId, String faction) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT my_faction FROM player_game_state WHERE game_id = ? AND player_id = ?",
                                String.class,
                                gameId,
                                playerId))
                        .isEqualTo(faction));
    }

    private void awaitLastRoundSummary(UUID gameId, int roundNumber) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT last_round_summary_round_number FROM game_projection WHERE game_id = ?",
                                Integer.class,
                                gameId))
                        .isEqualTo(roundNumber));
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
            assertThat(count).isEqualTo(size);
        });
    }

    private void awaitPendingHandSelectionCardCount(UUID gameId, UUID playerId, int size) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var stateId = jdbcTemplate.queryForObject(
                    "SELECT id FROM player_game_state WHERE game_id = ? AND player_id = ?",
                    UUID.class,
                    gameId,
                    playerId);
            var count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM player_game_state_pending_hand_card WHERE player_game_state_id = ?",
                    Integer.class,
                    stateId);
            assertThat(count).isEqualTo(size);
        });
    }

    private void awaitPhase(UUID gameId, String phase) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT phase FROM game_projection WHERE game_id = ?", String.class, gameId))
                        .isEqualTo(phase));
    }

    private void awaitEraAndPhase(UUID gameId, int eraNumber, String phase) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var state =
                    jdbcTemplate.queryForMap("SELECT era_number, phase FROM game_projection WHERE game_id = ?", gameId);
            assertThat(state).containsEntry("era_number", eraNumber);
            assertThat(state).containsEntry("phase", phase);
        });
    }

    private void awaitPendingParadoxCount(UUID gameId, int count) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_projection_pending_paradox WHERE game_projection_id = ?",
                                Integer.class,
                                gameId))
                        .isEqualTo(count));
    }

    private void awaitActiveEventRemoved(UUID gameId, UUID eventId) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_active_event WHERE game_id = ? AND event_id = ?",
                                Integer.class,
                                gameId,
                                eventId))
                        .isZero());
    }

    private void awaitActiveEventExists(UUID gameId, UUID eventId) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_active_event WHERE game_id = ? AND event_id = ?",
                                Integer.class,
                                gameId,
                                eventId))
                        .isEqualTo(1));
    }

    private void awaitScore(UUID gameId, UUID playerId, int score) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                "SELECT score FROM game_player WHERE game_id = ? AND player_id = ?",
                                Integer.class,
                                gameId,
                                playerId))
                        .isEqualTo(score));
    }

    private void awaitProbabilityIntelCount(UUID gameId, UUID playerId, int eraNumber, int expected) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThatProbabilityIntelCount(gameId, playerId, eraNumber, expected));
    }

    private void assertThatProbabilityIntelCount(UUID gameId, UUID playerId, int eraNumber, int expected) {
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM revealed_probability_intel "
                                + "WHERE game_id = ? AND player_id = ? AND era_number = ?",
                        Integer.class,
                        gameId,
                        playerId,
                        eraNumber))
                .isEqualTo(expected);
    }

    private void awaitProbabilityIntelProbability(
            UUID gameId, UUID playerId, int eraNumber, UUID eventId, int expectedProbability) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var probability = jdbcTemplate.queryForObject(
                    "SELECT (outcomes -> 0 ->> 'probability')::integer FROM revealed_probability_intel "
                            + "WHERE game_id = ? AND player_id = ? AND era_number = ? AND event_id = ?",
                    Integer.class,
                    gameId,
                    playerId,
                    eraNumber,
                    eventId);
            assertThat(probability).isEqualTo(expectedProbability);
        });
    }
}
