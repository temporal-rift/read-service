package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

record EraEndedPayload(UUID gameId, int eraNumber, int cascadedParadoxCount, int nextEraNumber) {}
