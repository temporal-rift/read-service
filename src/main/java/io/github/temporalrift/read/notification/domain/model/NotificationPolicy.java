package io.github.temporalrift.read.notification.domain.model;

import java.util.Map;
import java.util.Set;

public final class NotificationPolicy {

    private static final String PLAYER_ID_FIELD = "playerId";

    // Chain events carry the acting player's id, which would identify the Weavers faction holder before
    // FactionRevealed since only one player per game can hold it — these fields never reach a client.
    private static final Map<String, Set<String>> IDENTITY_REDACTIONS = Map.of(
            "ChainLinkAdded", Set.of(PLAYER_ID_FIELD),
            "ChainCompleted", Set.of(PLAYER_ID_FIELD),
            "ChainBroken", Set.of("brokenByPlayerId", "targetPlayerId"),
            "ChainLinkInvalidated", Set.of(PLAYER_ID_FIELD));

    private static final Set<String> TARGETED = Set.of(
            "FactionAssigned",
            "HandDealt",
            "HandSelected",
            "ProbabilityStateRevealed",
            "InfluenceTraced",
            "HandCardIntercepted",
            "PlayerJammed",
            "ThreadRejected");
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
            "AdjustedBandsPublished",
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
            "ChainBroken",
            "ChainLinkInvalidated");

    public Delivery deliveryFor(String eventType) {
        if (TARGETED.contains(eventType)) {
            return Delivery.TARGETED;
        }
        return BROADCAST.contains(eventType) ? Delivery.BROADCAST : Delivery.NEVER;
    }

    /** Payload field names to strip before delivery, empty when the event type carries no identity to redact. */
    public Set<String> identityFieldsToRedact(String eventType) {
        return IDENTITY_REDACTIONS.getOrDefault(eventType, Set.of());
    }

    public enum Delivery {
        BROADCAST,
        TARGETED,
        NEVER
    }
}
