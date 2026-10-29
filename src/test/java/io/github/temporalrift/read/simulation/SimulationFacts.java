package io.github.temporalrift.read.simulation;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;

/** Upstream records and execution contexts as an isolated simulation lane sends them to read-service. */
final class SimulationFacts {

    static final String GAME_EVENTS = "game.events";
    static final String TIMELINE_EVENTS = "timeline.events";

    // The contract's example rules and their RFC 8785 digest.
    static final String RULES = """
            {"schemaVersion":1,"properties":{"game.rules.probability.amplify-multiplier.II":2,\
            "game.rules.win-score-threshold":20}}""";
    static final String RULES_DIGEST = "2f4e77d6f2f5ad41eb3b05de091e524133995ef6f274a58f539adbf2d5b06fad";
    static final Instant LOGICAL_TIME = Instant.parse("2026-10-05T19:00:00Z");

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-05T19:00:00Z");

    private SimulationFacts() {}

    static String context(UUID caseKey) {
        return context(caseKey, "42");
    }

    static String context(UUID caseKey, String seed) {
        return """
                {"schemaVersion":1,"caseKey":"%s","seed":"%s","entropyVersion":"SHA256_V1",
                 "manifestDigest":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                 "rules":%s,"rulesDigest":"%s","logicalTime":"%s",
                 "seats":[{"seatIndex":0,"playerId":"0d9b6f5e-1f4a-4c62-9a3c-2d8f0e2b9d11","faction":"ERASERS"},
                          {"seatIndex":1,"playerId":"1e8c7a6f-2b5a-4d73-8b4d-3e9f1a3c5d22","faction":"WEAVERS"},
                          {"seatIndex":2,"playerId":"2f9d8b7a-3c6b-4e84-9c5e-4fa02b4d6f33","faction":"PROPHETS"}]}
                """.formatted(caseKey, seed, RULES, RULES_DIGEST, LOGICAL_TIME);
    }

    /** The records of one short game that touch every kind of projection read-service keeps. */
    static final class Game {

        final UUID gameId = UUID.randomUUID();
        final UUID player1 = UUID.randomUUID();
        final UUID player2 = UUID.randomUUID();
        final UUID eventId = UUID.randomUUID();
        final UUID outcomeId = UUID.randomUUID();
        final Fact started = Fact.of(
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
                        30));
        final Fact scored = Fact.of(
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
                                4))));
        final Fact ended = Fact.of(
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
                                Map.of("playerId", player2, "faction", "WEAVERS", "score", 0))));

        /** Records between the start and the end that fill the era, history and intel projections. */
        List<Fact> midGame() {
            var played = new HashMap<String, Object>();
            played.put("gameId", gameId);
            played.put("eraNumber", 1);
            played.put("roundNumber", 1);
            played.put("playerId", player1);
            played.put("cardInstanceId", UUID.randomUUID());
            played.put("cardType", "PUSH");
            played.put("grade", "II");
            played.put("targetEventId", eventId);
            played.put("sourceOutcomeId", null);
            played.put("targetOutcomeId", outcomeId);
            var card = Map.of("cardInstanceId", UUID.randomUUID(), "cardType", "SCAN", "grade", "I", "dealSlot", 1);
            return List.of(
                    Fact.of(
                            GAME_EVENTS,
                            "FactionAssigned",
                            gameId,
                            Map.of("gameId", gameId, "playerId", player1, "faction", "ERASERS")),
                    Fact.of(
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
                                    List.of(player1, player2))),
                    Fact.of(
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
                                                    "outcomeId",
                                                    outcomeId,
                                                    "description",
                                                    "d",
                                                    "initialProbability",
                                                    100)))))),
                    Fact.of(
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
                                    "2026-10-05T19:05:00Z",
                                    "cards",
                                    List.of(card))),
                    Fact.of(
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
                                    List.of(card))),
                    Fact.of(
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
                                    List.of(player1, player2))),
                    Fact.of(GAME_EVENTS, "CardPlayed", gameId, played),
                    Fact.of(
                            TIMELINE_EVENTS,
                            "ProbabilityStateRevealed",
                            gameId,
                            Map.of(
                                    "gameId",
                                    gameId,
                                    "eraNumber",
                                    1,
                                    "roundNumber",
                                    1,
                                    "playerId",
                                    player1,
                                    "eventId",
                                    eventId,
                                    "outcomes",
                                    List.of(Map.of(
                                            "outcomeId",
                                            outcomeId,
                                            "probability",
                                            40,
                                            "isAnnihilated",
                                            false,
                                            "isSealed",
                                            false)))),
                    Fact.of(GAME_EVENTS, "ResolutionStarted", gameId, Map.of("gameId", gameId, "eraNumber", 1)),
                    Fact.of(
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
                                    List.of())),
                    scored,
                    Fact.of(
                            GAME_EVENTS,
                            "EraEnded",
                            gameId,
                            Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2)));
        }
    }

    record Fact(String topic, String eventType, UUID gameId, UUID eventId, Object body) {

        static Fact of(String topic, String eventType, UUID gameId, Object body) {
            return new Fact(topic, eventType, gameId, UUID.randomUUID(), body);
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
    }
}
