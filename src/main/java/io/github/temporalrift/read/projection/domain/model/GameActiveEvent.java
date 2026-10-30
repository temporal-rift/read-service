package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An event still awaiting resolution — removed once its {@code OutcomeApplied} is consumed.
 *
 * @param revealIndex zero-based position of the event in the {@code EventsDrawn} that drew it into the current era;
 *     active events are listed in this order
 */
public record GameActiveEvent(
        UUID eventId, int revealIndex, String title, CarryOverState carryOverState, List<EventOutcome> outcomes) {

    public GameActiveEvent {
        Objects.requireNonNull(carryOverState, "carryOverState must not be null");
        if (revealIndex < 0) {
            throw new IllegalArgumentException("revealIndex must not be negative");
        }
        outcomes = List.copyOf(outcomes);
    }
}
