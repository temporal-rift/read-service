package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record OutcomeAppliedPayload(
        UUID gameId, int eraNumber, UUID eventId, UUID winningOutcomeId, List<FinalProbability> finalProbabilities) {

    record FinalProbability(UUID outcomeId, int probability) {}
}
