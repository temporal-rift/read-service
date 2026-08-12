package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** The cards dealt to one player for one era, retained independently of other players' hands. */
public record DealtHand(UUID playerId, List<DealtCard> cards) {

    public DealtHand {
        playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        cards = List.copyOf(cards);
    }
}
