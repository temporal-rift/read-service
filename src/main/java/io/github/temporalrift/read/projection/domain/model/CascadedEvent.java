package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** A complete cascaded-event history item materialized after metadata correlation. */
public record CascadedEvent(UUID eventId, String title) {

    public CascadedEvent {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        title = Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
