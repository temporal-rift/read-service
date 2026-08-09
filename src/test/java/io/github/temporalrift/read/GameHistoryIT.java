package io.github.temporalrift.read;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.shared.PlayerPrincipal;
import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class GameHistoryIT {

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void crossTopicArrival_buildsOrderedMultiEraHistoryAndServesGeneratedEndpoint() throws Exception {
        var gameId = UUID.randomUUID();
        var firstEventId = UUID.randomUUID();
        var firstOutcomeId = UUID.randomUUID();
        var secondEventId = UUID.randomUUID();
        var secondOutcomeId = UUID.randomUUID();

        publish(
                "game.events",
                "EventsDrawn",
                gameId,
                eventsDrawn(gameId, 1, firstEventId, firstOutcomeId, "First event", "First outcome"));
        awaitJsonArraySize(gameId, 1, "event_definitions", 1);
        publish(
                "timeline.events",
                "OutcomeApplied",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        1,
                        "eventId",
                        firstEventId,
                        "winningOutcomeId",
                        firstOutcomeId,
                        "finalProbabilities",
                        List.of()));

        // Reverse cross-topic arrival for era 2: retain the terminal fact before descriptive metadata.
        publish(
                "timeline.events",
                "ParadoxCascaded",
                gameId,
                Map.of(
                        "gameId",
                        gameId,
                        "eraNumber",
                        2,
                        "paradoxId",
                        UUID.randomUUID(),
                        "affectedEventId",
                        secondEventId,
                        "carryForwardProbabilityState",
                        List.of(Map.of("outcomeId", secondOutcomeId, "probability", 45))));
        awaitJsonArraySize(gameId, 2, "cascaded_event_references", 1);
        awaitCarryForwardState(gameId, 2, secondOutcomeId, 45);
        publish(
                "game.events",
                "EventsDrawn",
                gameId,
                eventsDrawn(gameId, 2, secondEventId, secondOutcomeId, "Second event", "Second outcome"));

        publish(
                "game.events",
                "EraEnded",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 1, "cascadedParadoxCount", 0, "nextEraNumber", 2));
        publish(
                "game.events",
                "EraEnded",
                gameId,
                Map.of("gameId", gameId, "eraNumber", 2, "cascadedParadoxCount", 1, "nextEraNumber", 3));
        awaitClosedEraCount(gameId, 2);
        awaitJsonArraySize(gameId, 1, "resolved_outcome_references", 1);

        mockMvc.perform(get("/api/v1/games/{gameId}/history", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(UUID.randomUUID())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                .andExpect(jsonPath("$.eras.length()").value(2))
                .andExpect(jsonPath("$.eras[0].eraNumber").value(1))
                .andExpect(jsonPath("$.eras[0].outcomes[0].eventId").value(firstEventId.toString()))
                .andExpect(jsonPath("$.eras[0].outcomes[0].title").value("First event"))
                .andExpect(jsonPath("$.eras[0].outcomes[0].winningOutcomeId").value(firstOutcomeId.toString()))
                .andExpect(jsonPath("$.eras[0].outcomes[0].winningOutcomeDescription")
                        .value("First outcome"))
                .andExpect(jsonPath("$.eras[0].paradoxesCascaded").value(0))
                .andExpect(jsonPath("$.eras[0].cascadedEvents").doesNotExist())
                .andExpect(jsonPath("$.eras[1].eraNumber").value(2))
                .andExpect(jsonPath("$.eras[1].outcomes").isEmpty())
                .andExpect(jsonPath("$.eras[1].paradoxesCascaded").value(1))
                .andExpect(jsonPath("$.eras[1].cascadedEvents[0].eventId").value(secondEventId.toString()))
                .andExpect(jsonPath("$.eras[1].cascadedEvents[0].title").value("Second event"));
    }

    @Test
    void noHistory_authenticatedRequest_returnsProblemDetails404() throws Exception {
        var gameId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/games/{gameId}/history", gameId)
                        .with(authentication(new PlayerAuthenticationToken(new PlayerPrincipal(UUID.randomUUID())))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Game history not found for game " + gameId));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/games/{gameId}/history", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> eventsDrawn(
            UUID gameId, int eraNumber, UUID eventId, UUID outcomeId, String title, String outcomeDescription) {
        return Map.of(
                "gameId",
                gameId,
                "eraNumber",
                eraNumber,
                "events",
                List.of(Map.of(
                        "eventId",
                        eventId,
                        "title",
                        title,
                        "carryOverState",
                        "FRESH",
                        "outcomes",
                        List.of(Map.of(
                                "outcomeId", outcomeId,
                                "description", outcomeDescription,
                                "initialProbability", 100)))));
    }

    private void publish(String topic, String eventType, UUID gameId, Object payload) {
        var message = MessageBuilder.withPayload((Object) objectMapper.writeValueAsBytes(payload))
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, gameId.toString())
                .setHeader("eventId", UUID.randomUUID().toString())
                .setHeader("eventType", eventType)
                .build();
        kafkaTemplate.send(message).join();
    }

    private void awaitJsonArraySize(UUID gameId, int eraNumber, String column, int expectedSize) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT COALESCE(MAX(jsonb_array_length(" + column
                                        + ")), -1) FROM game_history_projection WHERE game_id = ? AND era_number = ?",
                                Integer.class,
                                gameId,
                                eraNumber))
                        .isEqualTo(expectedSize));
    }

    private void awaitClosedEraCount(UUID gameId, int expectedCount) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM game_history_projection WHERE game_id = ? AND closed",
                                Integer.class,
                                gameId))
                        .isEqualTo(expectedCount));
    }

    private void awaitCarryForwardState(UUID gameId, int eraNumber, UUID outcomeId, int probability) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var state = jdbcTemplate.queryForMap("""
                    SELECT cascaded_event_references #>> '{0,carryForwardProbabilityState,0,outcomeId}' AS outcome_id,
                           cascaded_event_references #>> '{0,carryForwardProbabilityState,0,probability}' AS probability
                    FROM game_history_projection
                    WHERE game_id = ? AND era_number = ?
                    """, gameId, eraNumber);
            org.assertj.core.api.Assertions.assertThat(state)
                    .containsEntry("outcome_id", outcomeId.toString())
                    .containsEntry("probability", Integer.toString(probability));
        });
    }
}
