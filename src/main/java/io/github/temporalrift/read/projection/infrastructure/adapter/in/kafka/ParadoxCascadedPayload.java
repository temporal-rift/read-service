package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record ParadoxCascadedPayload(
        UUID gameId,
        int eraNumber,
        UUID paradoxId,
        UUID affectedEventId,
        List<CarryForwardProbability> carryForwardProbabilityState) {

    record CarryForwardProbability(UUID outcomeId, int probability) {}
}
