package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record GameEndedPayload(UUID gameId, String endReason, List<FinalScore> finalScores) {

    record FinalScore(UUID playerId, String faction, int score) {}
}
