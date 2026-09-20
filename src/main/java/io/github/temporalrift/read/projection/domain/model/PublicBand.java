package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** One public banded-probability event for a game era — approximate bands only, never exact weights. */
public record PublicBand(UUID gameId, int eraNumber, UUID eventId, int observedInRound, List<OutcomeBand> outcomes) {

    public PublicBand {
        outcomes = List.copyOf(outcomes);
    }

    public record OutcomeBand(UUID outcomeId, String band) {}
}
