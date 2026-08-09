package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** An outcome probability retained by a cascade for the next era. */
public record CarryForwardProbability(UUID outcomeId, int probability) {

    public CarryForwardProbability {
        outcomeId = Objects.requireNonNull(outcomeId, "outcomeId must not be null");
        if (probability < 0 || probability > 100) {
            throw new IllegalArgumentException("probability must be between 0 and 100");
        }
    }
}
