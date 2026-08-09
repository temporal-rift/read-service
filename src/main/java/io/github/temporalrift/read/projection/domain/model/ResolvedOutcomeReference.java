package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Terminal outcome identifiers retained until their {@code EventsDrawn} metadata is available. */
public record ResolvedOutcomeReference(UUID eventId, UUID winningOutcomeId) {

    public ResolvedOutcomeReference {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        winningOutcomeId = Objects.requireNonNull(winningOutcomeId, "winningOutcomeId must not be null");
    }
}
