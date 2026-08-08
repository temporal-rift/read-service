package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.CarryOverState;

record EventsDrawnPayload(UUID gameId, int eraNumber, List<DrawnEvent> events) {

    record DrawnEvent(UUID eventId, String title, List<DrawnOutcome> outcomes, CarryOverState carryOverState) {
        DrawnEvent {
            carryOverState = Objects.requireNonNull(carryOverState, "carryOverState must not be null");
        }
    }

    record DrawnOutcome(UUID outcomeId, String description, int initialProbability) {}
}
