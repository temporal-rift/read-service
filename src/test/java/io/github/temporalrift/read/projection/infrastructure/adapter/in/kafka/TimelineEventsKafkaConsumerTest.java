package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.AdjustedBandsPublishedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainBrokenPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainCompletedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainLinkAddedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainReAnchoredPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class TimelineEventsKafkaConsumerTest {

    @Mock
    ProcessedEventPort processedEvents;

    @Mock
    ProjectionEventApplier applier;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    KafkaSkipMetrics skipMetrics;

    @Test
    void handle_newEvent_claimsForTimelineEventsConsumer() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventId(eventId));

        then(processedEvents).should().claim(eventId, "projection.timeline-events");
    }

    @Test
    void handle_noEventTypeHeader_doesNotDispatch() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventId(eventId));

        verifyNoInteractions(applier);
    }

    @Test
    void handle_adjustedBandsPublished_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new AdjustedBandsPublishedPayload(UUID.randomUUID(), 1, List.of());
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "AdjustedBandsPublished")
                .setHeader("occurredAt", "2026-09-16T00:00:00Z")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, AdjustedBandsPublishedPayload.class))
                .willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        then(applier).should().applyAdjustedBandsPublished(payload);
    }

    @Test
    void handle_redeliveredAdjustedBandsPublished_isSkippedWithoutReclaiming() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(false);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventIdAndEventType(eventId, "AdjustedBandsPublished"));

        verifyNoInteractions(applier);
    }

    @Test
    void handle_chainLinkThreaded_isKnownButNotDispatched() {
        var eventId = UUID.randomUUID();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventIdAndEventType(eventId, "ChainLinkThreaded"));

        then(processedEvents).should().claim(eventId, "projection.timeline-events");
        verifyNoInteractions(applier);
    }

    @Test
    void handle_unrecognizedEventType_doesNotClaimOrThrow() {
        var eventId = UUID.randomUUID();

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics)
                .handle(KafkaTestMessages.withEventIdAndEventType(eventId, "SomeFutureEventType"));

        verifyNoInteractions(processedEvents, applier);
        then(skipMetrics).should().recordUnknownType();
    }

    @Test
    void handle_unsupportedVersion_doesNotClaimOrDispatch() {
        var eventId = UUID.randomUUID();
        var message = MessageBuilder.withPayload((Object) new byte[0])
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "OutcomeApplied")
                .setHeader("version", "2")
                .build();

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        verifyNoInteractions(processedEvents, applier);
        then(skipMetrics).should().recordUnsupportedVersion("projection.timeline-events");
    }

    @Test
    void handle_probabilityStateRevealed_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new ProbabilityStateRevealedPayload(
                UUID.randomUUID(), 2, 1, UUID.randomUUID(), UUID.randomUUID(), List.of());
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ProbabilityStateRevealed")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, ProbabilityStateRevealedPayload.class))
                .willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        then(applier).should().applyProbabilityStateRevealed(payload);
    }

    @Test
    void handle_chainLinkAdded_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new ChainLinkAddedPayload(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, null);
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ChainLinkAdded")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, ChainLinkAddedPayload.class)).willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        then(applier).should().applyChainLinkAdded(payload);
    }

    @Test
    void handle_chainCompleted_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new ChainCompletedPayload(UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), List.of());
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ChainCompleted")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, ChainCompletedPayload.class)).willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        then(applier).should().applyChainCompleted(payload);
    }

    @Test
    void handle_chainBroken_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new ChainBrokenPayload(
                UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2);
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ChainBroken")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, ChainBrokenPayload.class)).willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        then(applier).should().applyChainBroken(payload);
    }

    @Test
    void handle_chainReAnchored_dispatchesTheGeneratedPayload() {
        var eventId = UUID.randomUUID();
        var payload = new ChainReAnchoredPayload(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2);
        var message = MessageBuilder.withPayload((Object) payload)
                .setHeader("eventId", eventId.toString())
                .setHeader("eventType", "ChainReAnchored")
                .setHeader("version", "1")
                .build();
        given(processedEvents.claim(eventId, "projection.timeline-events")).willReturn(true);
        given(objectMapper.convertValue(payload, ChainReAnchoredPayload.class)).willReturn(payload);

        new TimelineEventsKafkaConsumer(processedEvents, applier, objectMapper, skipMetrics).handle(message);

        then(applier).should().applyChainReAnchored(payload);
    }
}
