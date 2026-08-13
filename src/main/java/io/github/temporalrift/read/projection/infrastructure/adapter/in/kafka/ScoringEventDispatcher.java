package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.EventHeaders;
import io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.ScoresUpdatedPayload;

/**
 * The {@code scoring-event} slice of {@code game.events} — one message type, and
 * {@link ProjectionEventApplier} projects it (design.md "Migration addendum: consumer contract adoption").
 */
class ScoringEventDispatcher
        implements io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.Consumer {

    private final ProjectionEventApplier applier;

    ScoringEventDispatcher(ProjectionEventApplier applier) {
        this.applier = applier;
    }

    @Override
    public void onScoresUpdated(ScoresUpdatedPayload payload, EventHeaders headers) {
        applier.applyScoresUpdated(payload);
    }
}
