package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** A complete history outcome materialized after terminal and descriptive facts are correlated. */
public record ResolvedOutcome(UUID eventId, String title, UUID winningOutcomeId, String winningOutcomeDescription) {

    public ResolvedOutcome {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        title = Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        winningOutcomeId = Objects.requireNonNull(winningOutcomeId, "winningOutcomeId must not be null");
        winningOutcomeDescription =
                Objects.requireNonNull(winningOutcomeDescription, "winningOutcomeDescription must not be null");
        if (winningOutcomeDescription.isBlank()) {
            throw new IllegalArgumentException("winningOutcomeDescription must not be blank");
        }
    }
}
