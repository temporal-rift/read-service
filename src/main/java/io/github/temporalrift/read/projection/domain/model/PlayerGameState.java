package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** A single player's private state within a game — the only genuinely per-viewer data (design.md Decision 1). */
public record PlayerGameState(
        UUID gameId,
        UUID playerId,
        String myFaction,
        List<HandCard> myHand,
        PendingHandSelection pendingHandSelection) {

    public PlayerGameState {
        myHand = List.copyOf(myHand);
    }

    public PlayerGameState(UUID gameId, UUID playerId, String myFaction, List<HandCard> myHand) {
        this(gameId, playerId, myFaction, myHand, null);
    }
}
