package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** A cascaded event identifier retained until its {@code EventsDrawn} metadata is available. */
public record CascadedEventReference(UUID eventId) {

    public CascadedEventReference {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
    }
}
