package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An event still awaiting resolution — removed once its {@code OutcomeApplied} is consumed. */
public record GameActiveEvent(UUID eventId, String title, CarryOverState carryOverState, List<EventOutcome> outcomes) {

    public GameActiveEvent {
        carryOverState = Objects.requireNonNull(carryOverState, "carryOverState must not be null");
        outcomes = List.copyOf(outcomes);
    }
}
