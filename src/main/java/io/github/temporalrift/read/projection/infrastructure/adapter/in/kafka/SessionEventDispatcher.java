package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraFailedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventHeaders;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionAssignedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionRevealedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameEndedAbnormallyPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartCancelledPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartFailedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HostTransferredPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.LobbyClosedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.LobbyCreatedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerAbandonedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerDisconnectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerJoinedLobbyPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerLeftLobbyPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ResolutionStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineCollapsedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineStabilizedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.WinConditionMetPayload;

/**
 * The {@code session-event} slice of {@code game.events} that {@link ProjectionEventApplier} actually
 * projects (design.md "Migration addendum: consumer contract adoption"). Every other {@code session-event}
 * message type is a real, named no-op — {@code session-event} carries lobby/win-condition/chain facts this
 * projection has no read model for, not messages this dispatcher failed to recognize.
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

    @Override
    public void onLobbyCreated(LobbyCreatedPayload payload, EventHeaders headers) {
        // Not projected — no read model needs pre-game lobby state.
    }

    @Override
    public void onPlayerJoinedLobby(PlayerJoinedLobbyPayload payload, EventHeaders headers) {
        // Not projected — no read model needs pre-game lobby state.
    }

    @Override
    public void onPlayerLeftLobby(PlayerLeftLobbyPayload payload, EventHeaders headers) {
        // Not projected — no read model needs pre-game lobby state.
    }

    @Override
    public void onHostTransferred(HostTransferredPayload payload, EventHeaders headers) {
        // Not projected — no read model needs pre-game lobby state.
    }

    @Override
    public void onLobbyClosed(LobbyClosedPayload payload, EventHeaders headers) {
        // Not projected — GameStarted is what actually creates this projection's rows.
    }

    @Override
    public void onFactionsDrawn(FactionsDrawnPayload payload, EventHeaders headers) {
        // Not projected — per-player FactionAssigned is what this projection reads instead.
    }

    @Override
    public void onGameStartFailed(GameStartFailedPayload payload, EventHeaders headers) {
        // Not projected — no game/rows exist yet for a start that never happened.
    }

    @Override
    public void onGameStartCancelled(GameStartCancelledPayload payload, EventHeaders headers) {
        // Not projected — no game/rows exist yet for a start that never happened.
    }

    @Override
    public void onEraFailed(EraFailedPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks era failure as distinct from ordinary era end.
    }

    @Override
    public void onGameEndedAbnormally(GameEndedAbnormallyPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks abnormal end as distinct from ordinary GameEnded.
    }

    @Override
    public void onTimelineCollapsed(TimelineCollapsedPayload payload, EventHeaders headers) {
        // Not projected — win/loss outcome isn't part of any current read model.
    }

    @Override
    public void onTimelineStabilized(TimelineStabilizedPayload payload, EventHeaders headers) {
        // Not projected — win/loss outcome isn't part of any current read model.
    }

    @Override
    public void onWinConditionMet(WinConditionMetPayload payload, EventHeaders headers) {
        // Not projected — win/loss outcome isn't part of any current read model.
    }
}
