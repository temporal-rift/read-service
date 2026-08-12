package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** One card dealt to a player for an era, as recorded in game-history. */
public record DealtCard(UUID cardInstanceId, String cardType) {

    public DealtCard {
        cardInstanceId = Objects.requireNonNull(cardInstanceId, "cardInstanceId must not be null");
        cardType = Objects.requireNonNull(cardType, "cardType must not be null");
    }
}
