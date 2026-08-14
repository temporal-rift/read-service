package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventHeaders;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionAssignedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionRevealedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerAbandonedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerDisconnectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ResolutionStartedPayload;

/**
 * The {@code session-event} slice of {@code game.events} that {@link ProjectionEventApplier} actually
 * projects (design.md "Migration addendum: consumer contract adoption"). Every other {@code session-event}
 * message type falls through to the generated {@code Consumer}'s default no-op — {@code session-event}
 * carries lobby/win-condition/chain facts this projection has no read model for.
 */
class SessionEventDispatcher
        implements io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.Consumer {

    private final ProjectionEventApplier applier;

    SessionEventDispatcher(ProjectionEventApplier applier) {
        this.applier = applier;
    }

    @Override
    public void onGameStarted(GameStartedPayload payload, EventHeaders headers) {
        applier.applyGameStarted(payload);
    }

    @Override
    public void onFactionAssigned(FactionAssignedPayload payload, EventHeaders headers) {
        applier.applyFactionAssigned(payload);
    }

    @Override
    public void onEraStarted(EraStartedPayload payload, EventHeaders headers) {
        applier.applyEraStarted(payload);
    }

    @Override
    public void onEventsDrawn(EventsDrawnPayload payload, EventHeaders headers) {
        applier.applyEventsDrawn(payload);
    }

    @Override
    public void onHandDealt(HandDealtPayload payload, EventHeaders headers) {
        applier.applyHandDealt(payload);
    }

    @Override
    public void onHandSelected(HandSelectedPayload payload, EventHeaders headers) {
        applier.applyHandSelected(payload);
    }

    @Override
    public void onPlayerDisconnected(PlayerDisconnectedPayload payload, EventHeaders headers) {
        applier.applyPlayerDisconnected(payload);
    }

    @Override
    public void onPlayerAbandoned(PlayerAbandonedPayload payload, EventHeaders headers) {
        applier.applyPlayerAbandoned(payload);
    }

    @Override
    public void onEraEnded(EraEndedPayload payload, EventHeaders headers) {
        applier.applyEraEnded(payload);
    }

    @Override
    public void onGameEnded(GameEndedPayload payload, EventHeaders headers) {
        applier.applyGameEnded(payload);
    }

    @Override
    public void onFactionRevealed(FactionRevealedPayload payload, EventHeaders headers) {
        applier.applyFactionRevealed(payload);
    }

    @Override
    public void onResolutionStarted(ResolutionStartedPayload payload, EventHeaders headers) {
        applier.applyResolutionStarted(payload);
    }
}
