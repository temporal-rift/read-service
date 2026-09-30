package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** One player's complete eligible resolution cards, including an explicitly empty offer. */
public record ResolutionCardOffer(UUID gameId, UUID playerId, int eraNumber, List<Card> cards) {
    public ResolutionCardOffer {
        cards = List.copyOf(cards);
    }

    public record Card(UUID cardInstanceId, String cardType, String grade) {}
}
