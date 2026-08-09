package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record ParadoxResolutionPhaseStartedPayload(UUID gameId, int eraNumber, List<UUID> paradoxIds, int timerSeconds) {}
