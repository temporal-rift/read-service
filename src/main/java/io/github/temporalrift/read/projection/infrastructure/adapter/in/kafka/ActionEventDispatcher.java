package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActivistDeclarationRecordedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.BandedProbabilityPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.Consumer;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.EventHeaders;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeBehaviorChangedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeSignatureRevealedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HandCardInterceptedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceTracedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ParadoxResolutionCardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PlayerJammedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SpecialActionPlayedPayload;

/**
 * The {@code action-event} slice of {@code game.events} that {@link ProjectionEventApplier} actually
 * projects (design.md "Migration addendum: consumer contract adoption"). Every other {@code action-event}
 * message type falls through to the generated {@code Consumer}'s default no-op — {@code action-event}
 * carries timer-expiry/skip/close facts this projection has no read model for.
 */
class ActionEventDispatcher implements Consumer {

    private final ProjectionEventApplier applier;

    ActionEventDispatcher(ProjectionEventApplier applier) {
        this.applier = applier;
    }

    @Override
    public void onActionRoundStarted(ActionRoundStartedPayload payload, EventHeaders headers) {
        applier.applyActionRoundStarted(payload, headers.occurredAt());
    }

    @Override
    public void onCardPlayed(CardPlayedPayload payload, EventHeaders headers) {
        applier.applyCardPlayed(payload);
    }

    @Override
    public void onParadoxResolutionCardPlayed(ParadoxResolutionCardPlayedPayload payload, EventHeaders headers) {
        applier.applyParadoxResolutionCardPlayed(payload);
    }

    @Override
    public void onSpecialActionPlayed(SpecialActionPlayedPayload payload, EventHeaders headers) {
        applier.applySpecialActionPlayed(payload);
    }

    @Override
    public void onRoundSummaryPublished(RoundSummaryPublishedPayload payload, EventHeaders headers) {
        applier.applyRoundSummaryPublished(payload);
    }

    @Override
    public void onPlayerJammed(PlayerJammedPayload payload, EventHeaders headers) {
        applier.applyPlayerJammed(payload);
    }

    @Override
    public void onInfluenceTraced(InfluenceTracedPayload payload, EventHeaders headers) {
        applier.applyInfluenceTraced(payload);
    }

    @Override
    public void onHandCardIntercepted(HandCardInterceptedPayload payload, EventHeaders headers) {
        applier.applyHandCardIntercepted(payload);
    }

    @Override
    public void onBandedProbabilityPublished(BandedProbabilityPublishedPayload payload, EventHeaders headers) {
        applier.applyBandedProbabilityPublished(payload);
    }

    @Override
    public void onActivistDeclarationRecorded(ActivistDeclarationRecordedPayload payload, EventHeaders headers) {
        applier.applyActivistDeclarationRecorded(payload);
    }

    @Override
    public void onExposeSignatureRevealed(ExposeSignatureRevealedPayload payload, EventHeaders headers) {
        applier.applyExposeSignatureRevealed(payload);
    }

    @Override
    public void onExposeBehaviorChanged(ExposeBehaviorChangedPayload payload, EventHeaders headers) {
        applier.applyExposeBehaviorChanged(payload);
    }
}
