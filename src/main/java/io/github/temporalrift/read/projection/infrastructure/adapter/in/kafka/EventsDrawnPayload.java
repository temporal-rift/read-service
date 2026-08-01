package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record EventsDrawnPayload(UUID gameId, int eraNumber, List<DrawnEvent> events) {

    record DrawnEvent(UUID eventId, String title, List<DrawnOutcome> outcomes, boolean isCascaded) {}

    record DrawnOutcome(UUID outcomeId, String description, int initialProbability) {}
}
