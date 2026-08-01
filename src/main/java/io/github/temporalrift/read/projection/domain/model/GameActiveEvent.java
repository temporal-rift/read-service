package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** An event still awaiting resolution — removed once its {@code OutcomeApplied} is consumed. */
public record GameActiveEvent(UUID eventId, String title, boolean isCascaded, List<EventOutcome> outcomes) {

    public GameActiveEvent {
        outcomes = List.copyOf(outcomes);
    }
}
