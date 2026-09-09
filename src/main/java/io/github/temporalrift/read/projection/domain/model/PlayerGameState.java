package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** A single player's private state within a game — the only genuinely per-viewer data (design.md Decision 1). */
public record PlayerGameState(
        UUID gameId,
        UUID playerId,
        String myFaction,
        List<HandCard> myHand,
        PendingHandSelection pendingHandSelection,
        Integer jammedEraNumber,
        Integer jammedUntilRound) {

    public PlayerGameState {
        myHand = List.copyOf(myHand);
    }

    public PlayerGameState(UUID gameId, UUID playerId, String myFaction, List<HandCard> myHand) {
        this(gameId, playerId, myFaction, myHand, null, null, null);
    }

    public PlayerGameState(
            UUID gameId,
            UUID playerId,
            String myFaction,
            List<HandCard> myHand,
            PendingHandSelection pendingHandSelection) {
        this(gameId, playerId, myFaction, myHand, pendingHandSelection, null, null);
    }

    /**
     * Null once the current era no longer matches the jam's era or the current round has passed the jammed-until
     * round — no explicit clear event is needed, since this is recomputed from state the projection already tracks.
     */
    public Integer effectiveJammedUntilRound(int currentEraNumber, Phase currentPhase) {
        if (jammedUntilRound == null || jammedEraNumber == null || jammedEraNumber != currentEraNumber) {
            return null;
        }
        return currentRound(currentPhase) <= jammedUntilRound ? jammedUntilRound : null;
    }

    private static int currentRound(Phase phase) {
        return switch (phase) {
            case ACTION_ROUND_1 -> 1;
            case ACTION_ROUND_2 -> 2;
            case ACTION_ROUND_3 -> 3;
            case LOBBY, ERA_START -> 0;
            case PARADOX_RESOLUTION, RESOLUTION, ERA_END, GAME_ENDED -> 4;
        };
    }
}
