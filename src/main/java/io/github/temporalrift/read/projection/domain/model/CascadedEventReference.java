package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** A cascaded event retained independently of descriptive metadata. */
public record CascadedEventReference(UUID eventId) {

    public CascadedEventReference {
        Objects.requireNonNull(eventId, "eventId must not be null");
    }
}
