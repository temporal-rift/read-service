package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.util.Set;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainBrokenPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainCompletedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainLinkAddedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolutionPhaseStartedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolvedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.MessageHeaders;

/** Consumes {@code timeline.events} (resolution facts from timeline-service) — design.md Decision 2/9. */
@Component
class TimelineEventsKafkaConsumer {

    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String CONSUMER = "projection.timeline-events";

    // Must be every type on timeline.events, not just the ones this consumer applies — narrowing this
    // would misclassify a legitimate, still-unapplied type as unsupported and skip it without claiming.
    private static final Set<String> KNOWN_EVENT_TYPES = Set.of(
            GeneratedChannelContract.RESOLUTION_STARTED_EVENT_TYPE,
            GeneratedChannelContract.PROBABILITY_STATE_CALCULATED_EVENT_TYPE,
            GeneratedChannelContract.PROBABILITY_STATE_REVEALED_EVENT_TYPE,
            GeneratedChannelContract.ADJUSTED_BANDS_PUBLISHED_EVENT_TYPE,
            GeneratedChannelContract.PARADOX_DETECTED_EVENT_TYPE,
            GeneratedChannelContract.PARADOX_RESOLUTION_PHASE_STARTED_EVENT_TYPE,
            GeneratedChannelContract.PARADOX_RESOLVED_EVENT_TYPE,
            GeneratedChannelContract.PARADOX_CASCADED_EVENT_TYPE,
            GeneratedChannelContract.OUTCOME_APPLIED_EVENT_TYPE,
            GeneratedChannelContract.ERA_RESOLUTION_COMPLETED_EVENT_TYPE,
            GeneratedChannelContract.CHAIN_LINK_THREADED_EVENT_TYPE,
            GeneratedChannelContract.CHAIN_LINK_ADDED_EVENT_TYPE,
            GeneratedChannelContract.CHAIN_COMPLETED_EVENT_TYPE,
            GeneratedChannelContract.CHAIN_BROKEN_EVENT_TYPE,
            GeneratedChannelContract.CHAIN_LINK_INVALIDATED_EVENT_TYPE,
            GeneratedChannelContract.THREAD_REJECTED_EVENT_TYPE,
            GeneratedChannelContract.CORRUPT_INVERSION_CONFIRMED_EVENT_TYPE,
            GeneratedChannelContract.RESOLUTION_FAILED_EVENT_TYPE,
            GeneratedChannelContract.RESOLUTION_WARNING_EVENT_TYPE);

    private final ProcessedEventPort processedEvents;
    private final ProjectionEventApplier applier;
    private final ObjectMapper objectMapper;
    private final KafkaSkipMetrics skipMetrics;

    TimelineEventsKafkaConsumer(
            ProcessedEventPort processedEvents,
            ProjectionEventApplier applier,
            ObjectMapper objectMapper,
            KafkaSkipMetrics skipMetrics) {
        this.processedEvents = processedEvents;
        this.applier = applier;
        this.objectMapper = objectMapper;
        this.skipMetrics = skipMetrics;
    }

    @KafkaListener(topics = "timeline.events", groupId = "read-service." + CONSUMER)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(Message<Object> message) {
        UnsupportedEventGate.accept(
                        message,
                        CONSUMER,
                        KNOWN_EVENT_TYPES,
                        UnsupportedEventGate.CURRENT_ENVELOPE_VERSION,
                        skipMetrics,
                        processedEvents)
                .ifPresent(eventId -> dispatch(message));
    }

    private void dispatch(Message<Object> message) {
        var eventType = MessageHeaders.asString(message, EVENT_TYPE_HEADER);
        switch (eventType) {
            case null -> {
                // No eventType header — nothing to dispatch.
            }
            case "OutcomeApplied" -> applier.applyOutcomeApplied(read(message, OutcomeAppliedPayload.class));
            case "ParadoxResolutionPhaseStarted" ->
                applier.applyParadoxResolutionPhaseStarted(read(message, ParadoxResolutionPhaseStartedPayload.class));
            case "ParadoxResolved" -> applier.applyParadoxResolved(read(message, ParadoxResolvedPayload.class));
            case "ParadoxCascaded" -> applier.applyParadoxCascaded(read(message, ParadoxCascadedPayload.class));
            case "ProbabilityStateRevealed" ->
                applier.applyProbabilityStateRevealed(read(message, ProbabilityStateRevealedPayload.class));
            case "ChainLinkAdded" -> applier.applyChainLinkAdded(read(message, ChainLinkAddedPayload.class));
            case "ChainCompleted" -> applier.applyChainCompleted(read(message, ChainCompletedPayload.class));
            case "ChainBroken" -> applier.applyChainBroken(read(message, ChainBrokenPayload.class));
            default -> {
                // Other timeline.events types aren't consumed by this projection.
            }
        }
    }

    private <T> T read(Message<Object> message, Class<T> type) {
        return GameEventPayloads.read(objectMapper, message.getPayload(), type);
    }
}
