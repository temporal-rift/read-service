package io.github.temporalrift.read.notification.domain.model;

import java.util.Set;

public final class NotificationPolicy {

    private static final Set<String> TARGETED = Set.of("FactionAssigned", "HandDealt");
    private static final Set<String> NEVER = Set.of(
            "CardPlayed",
            "SpecialActionPlayed",
            "ParadoxResolutionCardPlayed",
            "ProbabilityStateCalculated",
            "EraResolutionCompleted");
    private static final Set<String> BROADCAST = Set.of(
            "LobbyCreated",
            "PlayerJoinedLobby",
            "PlayerLeftLobby",
            "HostTransferred",
            "LobbyClosed",
            "FactionsDrawn",
            "GameStarted",
            "GameStartFailed",
            "GameStartCancelled",
            "PlayerDisconnected",
            "PlayerAbandoned",
            "EraStarted",
            "EventsDrawn",
            "ActionRoundStarted",
            "ActivistDeclarationRecorded",
            "ExposeSignatureRevealed",
            "ExposeBehaviorChanged",
            "ActionRoundTimerExpired",
            "PlayerSkipped",
            "ActionRoundClosed",
            "RoundSummaryPublished",
            "ResolutionStarted",
            "BandedProbabilityPublished",
            "ParadoxDetected",
            "ParadoxResolutionPhaseStarted",
            "ParadoxResolved",
            "ParadoxCascaded",
            "OutcomeApplied",
            "ScoresUpdated",
            "EraEnded",
            "WinConditionMet",
            "TimelineCollapsed",
            "TimelineStabilized",
            "EraFailed",
            "GameEndedAbnormally",
            "GameEnded",
            "FactionRevealed",
            "ChainLinkAdded",
            "ChainCompleted",
            "ChainBroken");

    public Delivery deliveryFor(String eventType) {
        if (TARGETED.contains(eventType)) {
            return Delivery.TARGETED;
        }
        return BROADCAST.contains(eventType) ? Delivery.BROADCAST : Delivery.NEVER;
    }

    public enum Delivery {
        BROADCAST,
        TARGETED,
        NEVER
    }
}
