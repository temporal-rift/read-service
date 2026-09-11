package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * An intercepting viewer's point-in-time hand-card reveal for one observed player in one era.
 * An intercept observes a hand, not a game event, so the intel identity's {@code eventId}
 * carries the observed {@code targetPlayerId}.
 */
public record RevealedHandCardIntel(
        UUID gameId,
        UUID playerId,
        int eraNumber,
        UUID targetPlayerId,
        int observedInRound,
        List<RevealedHandCard> revealedCards)
        implements RevealedIntelEntry {

    public RevealedHandCardIntel {
        revealedCards = List.copyOf(revealedCards);
    }

    @Override
    public UUID eventId() {
        return targetPlayerId();
    }
}
