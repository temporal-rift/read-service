package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** The public detail of one paradox detected in a game's era, shared with every participant. */
public record DetectedParadox(
        UUID gameId,
        int eraNumber,
        UUID paradoxId,
        ParadoxType type,
        UUID affectedEventId,
        List<UUID> affectedOutcomeIds) {

    public DetectedParadox {
        affectedOutcomeIds = List.copyOf(affectedOutcomeIds);
    }
}
