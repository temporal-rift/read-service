package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record ActionRoundStartedPayload(
        UUID gameId, int eraNumber, int roundNumber, int timerSeconds, List<UUID> pendingPlayerIds) {}
