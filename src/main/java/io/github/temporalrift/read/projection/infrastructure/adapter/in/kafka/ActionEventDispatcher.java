package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.EventHeaders;

/**
 * The {@code action-event} slice of {@code game.events} that {@link ProjectionEventApplier} actually
 * projects (design.md "Migration addendum: consumer contract adoption"). Every other {@code action-event}
 * message type falls through to the generated {@code Consumer}'s default no-op — {@code action-event}
 * carries per-card-modifier and expose facts this projection has no read model for.
 */
class ActionEventDispatcher implements io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.Consumer {

    private final ProjectionEventApplier applier;

    ActionEventDispatcher(ProjectionEventApplier applier) {
        this.applier = applier;
    }

    @Override
    public void onActionRoundStarted(ActionRoundStartedPayload payload, EventHeaders headers) {
        applier.applyActionRoundStarted(payload);
    }

    @Override
    public void onCardPlayed(CardPlayedPayload payload, EventHeaders headers) {
        applier.applyCardPlayed(payload);
    }
}
