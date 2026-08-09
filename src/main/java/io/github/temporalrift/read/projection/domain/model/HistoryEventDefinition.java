package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable descriptive metadata and reveal position supplied by {@code EventsDrawn}. */
public record HistoryEventDefinition(UUID eventId, int revealIndex, String title, List<EventOutcome> outcomes) {

    public HistoryEventDefinition {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        if (revealIndex < 0) {
            throw new IllegalArgumentException("revealIndex must not be negative");
        }
        title = Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        outcomes = List.copyOf(outcomes);
        if (outcomes.stream()
                .anyMatch(outcome -> outcome.outcomeId() == null
                        || outcome.description() == null
                        || outcome.description().isBlank())) {
            throw new IllegalArgumentException("outcomes must contain IDs and nonblank descriptions");
        }
    }
}
