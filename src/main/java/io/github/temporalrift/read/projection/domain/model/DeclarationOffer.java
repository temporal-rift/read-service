package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** One participant's server-owned declaration eligibility for an open era window. */
public record DeclarationOffer(UUID gameId, UUID playerId, int eraNumber, List<Mode> eligibleModes) {
    public DeclarationOffer {
        eligibleModes = List.copyOf(eligibleModes);
        if (eligibleModes.isEmpty()) {
            throw new IllegalArgumentException("eligibleModes must not be empty");
        }
    }

    public enum Mode {
        RALLY,
        MOMENTUM
    }
}
