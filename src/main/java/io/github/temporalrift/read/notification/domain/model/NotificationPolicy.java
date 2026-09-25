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
    private static final String EVENTS_FIELD = "events";
    private static final String OUTCOMES_FIELD = "outcomes";
    private static final String CARRY_OVER_STATE_FIELD = "carryOverState";
    private static final String CARRY_FORWARD_PROBABILITY_STATE_FIELD = "carryForwardProbabilityState";

    // Chain events carry the acting player's id, which would identify the Weavers faction holder before
    // FactionRevealed since only one player per game can hold it — these fields never reach a client.
    // ChainBroken also strips its retired pre-5.0 fields: a retained or replayed record from the previous
    // contract revision can still arrive with those names, and the notification path forwards the raw
    // payload without schema validation.
    private static final Map<String, Set<String>> IDENTITY_REDACTIONS = Map.of(
            "ChainLinkAdded", Set.of(PLAYER_ID_FIELD),
            "ChainCompleted", Set.of(PLAYER_ID_FIELD),
            "ChainBroken", Set.of(PLAYER_ID_FIELD, "brokenByPlayerId", "targetPlayerId"),
            "ChainLinkInvalidated", Set.of(PLAYER_ID_FIELD));

    private static final Set<String> TARGETED = Set.of(
            "FactionAssigned",
            "HandDealt",
            "HandSelected",
            "ProbabilityStateRevealed",
            "InfluenceTraced",
            "HandCardIntercepted",
            "ForesightRevealed",
            "PlayerJammed",
            "ThreadRejected");
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

    /** Returns the event's safe public view without mutating the consumed payload. */
    public JsonNode publicPayloadFor(String eventType, JsonNode payload) {
        if (!(payload instanceof ObjectNode objectPayload)) {
            return payload;
        }
        var publicPayload = objectPayload.deepCopy();
        identityFieldsToRedact(eventType).forEach(publicPayload::remove);
        if ("ParadoxCascaded".equals(eventType)) {
            publicPayload.remove(CARRY_FORWARD_PROBABILITY_STATE_FIELD);
        } else if ("EventsDrawn".equals(eventType)) {
            redactCarriedEventWeights(publicPayload);
        }
        return publicPayload;
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

    private static void redactCarriedEventWeights(ObjectNode payload) {
        if (!(payload.get(EVENTS_FIELD) instanceof ArrayNode events)) {
            return;
        }
        events.forEach(event -> {
            if (!(event instanceof ObjectNode eventPayload) || isFresh(eventPayload)) {
                return;
            }
            if (eventPayload.get(OUTCOMES_FIELD) instanceof ArrayNode outcomes) {
                outcomes.forEach(NotificationPolicy::redactOutcomeWeights);
            }
        });
    }

    private static boolean isFresh(ObjectNode eventPayload) {
        return eventPayload.hasNonNull(CARRY_OVER_STATE_FIELD)
                && "FRESH".equals(eventPayload.get(CARRY_OVER_STATE_FIELD).asText());
    }

    private static void redactOutcomeWeights(JsonNode outcome) {
        if (outcome instanceof ObjectNode outcomePayload) {
            outcomePayload.remove("initialProbability");
            outcomePayload.remove("probability");
        }
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
