package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** Game-scoped, shared-across-players state (design.md Decision 1). */
public record GameProjection(UUID gameId, int eraNumber, Phase phase) {}
