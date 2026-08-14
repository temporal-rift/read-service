package io.github.temporalrift.read.projection.domain.model;

import java.time.Instant;
import java.util.List;

/** The player's private choice of a final action hand from a dealt card pool. */
public record PendingHandSelection(List<PendingHandCard> cards, Instant expiresAt) {

    public PendingHandSelection {
        cards = List.copyOf(cards);
    }
}
