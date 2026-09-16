package io.github.temporalrift.read.notification.domain.model;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class NotificationPolicy {

    private static final String PLAYER_ID_FIELD = "playerId";
    private static final String UPDATES_FIELD = "updates";
    private static final String FACTION_FIELD = "faction";
    private static final String REASON_FIELD = "reason";

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

    /**
     * Reshapes a ScoresUpdated payload for one viewer: an {@code updates} entry keeps its {@code faction} and
     * {@code reason} only when it belongs to {@code viewerPlayerId}; every other entry has both stripped so a
     * hidden faction never leaks through score history. Payloads without an {@code updates} array pass through
     * unchanged.
     */
    public JsonNode scoresUpdatedFor(JsonNode payload, UUID viewerPlayerId) {
        if (!(payload instanceof ObjectNode objectPayload)
                || !(objectPayload.get(UPDATES_FIELD) instanceof ArrayNode updates)) {
            return payload;
        }
        var filtered = objectPayload.deepCopy();
        var filteredUpdates = filtered.arrayNode();
        updates.forEach(update -> filteredUpdates.add(viewOf(update, viewerPlayerId)));
        filtered.set(UPDATES_FIELD, filteredUpdates);
        return filtered;
    }

    private static JsonNode viewOf(JsonNode update, UUID viewerPlayerId) {
        if (!(update instanceof ObjectNode objectUpdate) || belongsTo(objectUpdate, viewerPlayerId)) {
            return update;
        }
        var redacted = objectUpdate.deepCopy();
        redacted.remove(FACTION_FIELD);
        redacted.remove(REASON_FIELD);
        return redacted;
    }

    private static boolean belongsTo(ObjectNode update, UUID viewerPlayerId) {
        return viewerPlayerId != null
                && update.hasNonNull(PLAYER_ID_FIELD)
                && viewerPlayerId.toString().equals(update.get(PLAYER_ID_FIELD).asText());
    }

    public enum Delivery {
        BROADCAST,
        TARGETED,
        NEVER
    }
}
