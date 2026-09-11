package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** A viewer's latest exact probability state for one scanned event in one era. */
public record RevealedProbabilityIntel(
        UUID gameId,
        UUID playerId,
        int eraNumber,
        UUID eventId,
        int observedInRound,
        List<RevealedProbabilityOutcome> outcomes)
        implements RevealedIntelEntry {

    public RevealedProbabilityIntel {
        outcomes = List.copyOf(outcomes);
    }
}
