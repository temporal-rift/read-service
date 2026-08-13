package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundClosedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundTimerExpiredPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActivistDeclarationRecordedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.BandedProbabilityPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.EventHeaders;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeBehaviorChangedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeSignatureRevealedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ParadoxResolutionCardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PlayerSkippedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SpecialActionPlayedPayload;

/**
 * The {@code action-event} slice of {@code game.events} that {@link ProjectionEventApplier} actually
 * projects (design.md "Migration addendum: consumer contract adoption"). Every other {@code action-event}
 * message type is a real, named no-op — {@code action-event} carries per-card-modifier and expose facts this
 * projection has no read model for, not messages this dispatcher failed to recognize.
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

    @Override
    public void onParadoxResolutionCardPlayed(ParadoxResolutionCardPlayedPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks in-flight paradox resolution card plays.
    }

    @Override
    public void onSpecialActionPlayed(SpecialActionPlayedPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks special-action plays.
    }

    @Override
    public void onActionRoundTimerExpired(ActionRoundTimerExpiredPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks round-timer expiry as distinct from the round ending.
    }

    @Override
    public void onPlayerSkipped(PlayerSkippedPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks per-round skip reasons.
    }

    @Override
    public void onActionRoundClosed(ActionRoundClosedPayload payload, EventHeaders headers) {
        // Not projected — ActionRoundStarted is what advances the projected phase instead.
    }

    @Override
    public void onRoundSummaryPublished(RoundSummaryPublishedPayload payload, EventHeaders headers) {
        // Not projected — no read model surfaces round action summaries.
    }

    @Override
    public void onBandedProbabilityPublished(BandedProbabilityPublishedPayload payload, EventHeaders headers) {
        // Not projected — no read model surfaces banded probability state.
    }

    @Override
    public void onActivistDeclarationRecorded(ActivistDeclarationRecordedPayload payload, EventHeaders headers) {
        // Not projected — no read model tracks Activist declarations.
    }

    @Override
    public void onExposeSignatureRevealed(ExposeSignatureRevealedPayload payload, EventHeaders headers) {
        // Not projected — delivered to players over the notification module, not this projection.
    }

    @Override
    public void onExposeBehaviorChanged(ExposeBehaviorChangedPayload payload, EventHeaders headers) {
        // Not projected — delivered to players over the notification module, not this projection.
    }
}
