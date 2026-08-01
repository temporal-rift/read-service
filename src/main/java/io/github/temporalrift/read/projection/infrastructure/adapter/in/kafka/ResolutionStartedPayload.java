package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

record ResolutionStartedPayload(UUID gameId, int eraNumber) {}
