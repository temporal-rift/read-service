package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A cascaded event and its ordered carry-forward state retained independently of descriptive metadata. */
public record CascadedEventReference(UUID eventId, List<CarryForwardProbability> carryForwardProbabilityState) {

    public CascadedEventReference {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        carryForwardProbabilityState = List.copyOf(carryForwardProbabilityState);
    }
}
