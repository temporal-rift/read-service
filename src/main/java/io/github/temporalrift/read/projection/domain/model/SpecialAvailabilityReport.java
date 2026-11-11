package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * A player's private special-action availability for one action round. Only the latest round per game and
 * player is kept; "latest" orders by era first and round second.
 */
public record SpecialAvailabilityReport(
        UUID gameId, UUID playerId, int eraNumber, int roundNumber, List<SpecialAvailability> specials) {

    public SpecialAvailabilityReport {
        specials = List.copyOf(specials);
    }

    /** True when this report is for a strictly earlier round than {@code other}, so it must not replace it. */
    public boolean isOlderThan(SpecialAvailabilityReport other) {
        return eraNumber < other.eraNumber || (eraNumber == other.eraNumber && roundNumber < other.roundNumber);
    }

    public boolean isFor(int currentEraNumber, int currentRoundNumber) {
        return eraNumber == currentEraNumber && roundNumber == currentRoundNumber;
    }
}
