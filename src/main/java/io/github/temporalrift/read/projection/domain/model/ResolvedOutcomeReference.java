package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Terminal outcome identifiers retained until their {@code EventsDrawn} metadata is available. */
public record ResolvedOutcomeReference(UUID eventId, UUID winningOutcomeId) {

    public ResolvedOutcomeReference {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(winningOutcomeId, "winningOutcomeId must not be null");
    }
}
