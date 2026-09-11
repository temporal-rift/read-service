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

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HandCardInterceptedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceTracedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class GameEventsKafkaConsumerTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Mock
    ProjectionEventApplier applier;

    @Mock
    ObjectMapper objectMapper;

    @Test
    void handle_newEvent_claimsForGameEventsConsumer() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper)
                .handle(KafkaTestMessages.withEventId(eventId));

        then(processedEvents).should().claim(eventId, "projection.game-events");
    }

    @Test
    void handle_noBindingNameHeader_doesNotDispatch() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper)
                .handle(KafkaTestMessages.withEventId(eventId));

        verifyNoInteractions(applier);
    }

    @Test
    void handle_eventTypeHeaderAsString_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper)
                .handle(KafkaTestMessages.withEventIdAndEventType(eventId, "GameStarted"));

        then(applier).should().applyGameStarted(any());
    }

    @Test
    void handle_eventTypeHeaderAsRawBytes_dispatchesToApplier() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, objectMapper)
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
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper()).handle(message);

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
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper()).handle(message);

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
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper()).handle(message);

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
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper()).handle(message);

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
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper()).handle(message);

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
                .build();
        given(processedEvents.claim(eventId, "projection.game-events")).willReturn(true);

        new GameEventsKafkaConsumer(processedEvents, applier, new ObjectMapper()).handle(message);

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
}
