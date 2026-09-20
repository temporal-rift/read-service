package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HandCardInterceptedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceTracedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SpecialActionPlayedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.WinConditionMetPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class GameEventsKafkaConsumerTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Mock
    ProjectionEventApplier applier;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    KafkaSkipMetrics skipMetrics;

    @Test
    void handle_newEvent_claimsForGameEventsConsumer() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventId(eventId));

        then(processedEvents).should().claim(eventId, "projection.game-events");
    }

    @Test
    void handle_noBindingNameHeader_doesNotDispatch() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventId(eventId));

        verifyNoInteractions(applier);
    }

    @Test
    void handle_unrecognizedEventType_doesNotClaimOrThrow() {
        var eventId = UUID.randomUUID();

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventIdAndEventType(eventId, "SomeFutureEventType"));

        verifyNoInteractions(processedEvents, applier);
        then(skipMetrics).should().recordUnknownType();
    }

    @Test
    void handle_knownButUnprojectedEventType_claimsButNeverDeserializesOrDispatches() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);
        // Malformed for LobbyCreatedPayload on purpose: if dispatch() still routed this through the
        // generated session dispatcher's eager deserialization, reading it would throw.
        var message = MessageBuilder.withPayload((Object) "not valid json".getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "LobbyCreated")
                .setHeader("version", "1")
                .build();

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        then(processedEvents).should().claim(eventId, "projection.game-events");
        verifyNoInteractions(applier);
    }

    @Test
    void handle_unsupportedVersion_doesNotClaimOrDispatch() {
        var eventId = UUID.randomUUID();
        var message = MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "GameStarted")
                .setHeader("version", "2")
                .build();

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        verifyNoInteractions(processedEvents, applier);
        then(skipMetrics).should().recordUnsupportedVersion("projection.game-events");
    }

    @Test
    void handle_eventTypeHeaderAsString_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventIdAndEventType(eventId, "GameStarted"));

        then(applier).should().applyGameStarted(any());
    }

    @Test
    void handle_eventTypeHeaderAsRawBytes_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventIdAndEventType(
                        eventId, "GameStarted".getBytes(StandardCharsets.UTF_8)));

        then(applier).should().applyGameStarted(any());
    }

    @Test
    void handle_playerTargetingCardPlayedWithoutTargetEventId_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 2,
                  "playerId": "%s",
                  "cardInstanceId": "%s",
                  "cardType": "NULLIFY",
                  "grade": "I",
                  "targetPlayerId": "%s"
                }
                """.formatted(gameId, playerId, cardInstanceId, targetPlayerId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "CardPlayed")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(CardPlayedPayload.class);
        then(applier).should().applyCardPlayed(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().targetEventId()).isNull();
        assertThat(payloadCaptor.getValue().targetPlayerId()).isEqualTo(targetPlayerId);
        assertThat(payloadCaptor.getValue().cardInstanceId()).isEqualTo(cardInstanceId);
    }

    @Test
    void handle_multiTargetCardPlayed_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();
        var targetEventIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        var payload =
                """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 2,
                  "playerId": "%s",
                  "cardInstanceId": "%s",
                  "cardType": "SCAN",
                  "grade": "II",
                  "targetEventIds": ["%s", "%s"]
                }
                """.formatted(gameId, playerId, cardInstanceId, targetEventIds.getFirst(), targetEventIds.getLast());
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "CardPlayed")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(CardPlayedPayload.class);
        then(applier).should().applyCardPlayed(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().targetEventIds()).containsExactlyElementsOf(targetEventIds);
        assertThat(payloadCaptor.getValue().targetEventId()).isNull();
    }

    @Test
    void handle_roundSummaryPublished_dispatchesOnlyItsPublicPayloadToTheApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 2,
                  "actionSummaries": [{
                    "playerId": "%s",
                    "actionCategory": "INFORMATION",
                    "actionFamily": "CARD",
                    "skipped": false
                  }]
                }
                """.formatted(gameId, playerId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "RoundSummaryPublished")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(RoundSummaryPublishedPayload.class);
        then(applier).should().applyRoundSummaryPublished(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().roundNumber()).isEqualTo(2);
        assertThat(payloadCaptor.getValue().actionSummaries()).singleElement().satisfies(summary -> {
            assertThat(summary.playerId()).isEqualTo(playerId);
            assertThat(summary.actionCategory()).isEqualTo("INFORMATION");
            assertThat(summary.actionFamily()).isEqualTo("CARD");
            assertThat(summary.skipped()).isFalse();
        });
    }

    @Test
    void handle_influenceTraced_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var influencerId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 2,
                  "playerId": "%s",
                  "targetEventId": "%s",
                  "influencerPlayerIds": ["%s"]
                }
                """.formatted(gameId, viewerId, targetEventId, influencerId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "InfluenceTraced")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(InfluenceTracedPayload.class);
        then(applier).should().applyInfluenceTraced(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().playerId()).isEqualTo(viewerId);
        assertThat(payloadCaptor.getValue().targetEventId()).isEqualTo(targetEventId);
        assertThat(payloadCaptor.getValue().influencerPlayerIds()).containsExactly(influencerId);
    }

    @Test
    void handle_influenceTracedWithEmptyInfluencers_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 1,
                  "playerId": "%s",
                  "targetEventId": "%s",
                  "influencerPlayerIds": []
                }
                """.formatted(gameId, viewerId, targetEventId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "InfluenceTraced")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(InfluenceTracedPayload.class);
        then(applier).should().applyInfluenceTraced(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().influencerPlayerIds()).isEmpty();
    }

    @Test
    void handle_handCardIntercepted_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var viewerId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 2,
                  "playerId": "%s",
                  "targetPlayerId": "%s",
                  "revealedCards": [{
                    "cardInstanceId": "%s",
                    "cardType": "SWING",
                    "grade": "III"
                  }]
                }
                """.formatted(gameId, viewerId, targetPlayerId, cardInstanceId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "HandCardIntercepted")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(HandCardInterceptedPayload.class);
        then(applier).should().applyHandCardIntercepted(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().playerId()).isEqualTo(viewerId);
        assertThat(payloadCaptor.getValue().targetPlayerId()).isEqualTo(targetPlayerId);
        assertThat(payloadCaptor.getValue().revealedCards()).singleElement().satisfies(card -> {
            assertThat(card.cardInstanceId()).isEqualTo(cardInstanceId);
            assertThat(card.cardType().name()).isEqualTo("SWING");
            assertThat(card.grade().name()).isEqualTo("III");
        });
    }

    @Test
    void handle_actionRoundStarted_forwardsOccurredAtForDeadlineComputation() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 2,
                  "timerSeconds": 45,
                  "pendingPlayerIds": []
                }
                """.formatted(gameId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ActionRoundStarted")
                .setHeader("occurredAt", "2026-09-16T00:00:00Z")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(ActionRoundStartedPayload.class);
        var occurredAtCaptor = ArgumentCaptor.forClass(java.time.Instant.class);
        then(applier).should().applyActionRoundStarted(payloadCaptor.capture(), occurredAtCaptor.capture());
        assertThat(payloadCaptor.getValue().gameId()).isEqualTo(gameId);
        assertThat(payloadCaptor.getValue().roundNumber()).isEqualTo(2);
        assertThat(occurredAtCaptor.getValue()).isEqualTo(java.time.Instant.parse("2026-09-16T00:00:00Z"));
    }

    @Test
    void handle_specialActionPlayed_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "eraNumber": 1,
                  "roundNumber": 1,
                  "playerId": "%s",
                  "faction": "ERASERS",
                  "specialAction": "ANNIHILATE"
                }
                """.formatted(gameId, playerId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "SpecialActionPlayed")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(SpecialActionPlayedPayload.class);
        then(applier).should().applySpecialActionPlayed(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().playerId()).isEqualTo(playerId);
        assertThat(payloadCaptor.getValue().specialAction().name()).isEqualTo("ANNIHILATE");
    }

    @Test
    void handle_winConditionMet_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        var gameId = UUID.randomUUID();
        var winnerId = UUID.randomUUID();
        var payload = """
                {
                  "gameId": "%s",
                  "winnerId": "%s",
                  "faction": "WEAVERS",
                  "finalScore": 20,
                  "winType": "SCORE_THRESHOLD"
                }
                """.formatted(gameId, winnerId);
        var message = MessageBuilder.withPayload((Object) payload.getBytes(StandardCharsets.UTF_8))
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "WinConditionMet")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper(), skipMetrics).handle(message);

        var payloadCaptor = ArgumentCaptor.forClass(WinConditionMetPayload.class);
        then(applier).should().applyWinConditionMet(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().winnerId()).isEqualTo(winnerId);
    }
}
