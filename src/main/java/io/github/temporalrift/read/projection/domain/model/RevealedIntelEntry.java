package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** A viewer's current-era intel entry served in {@code myRevealedIntel}. */
public sealed interface RevealedIntelEntry
        permits RevealedProbabilityIntel, RevealedInfluenceIntel, RevealedHandCardIntel {

    UUID gameId();

    UUID playerId();

    int eraNumber();

    UUID eventId();

    int observedInRound();
}
