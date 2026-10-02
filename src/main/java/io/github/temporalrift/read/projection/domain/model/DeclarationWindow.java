package io.github.temporalrift.read.projection.domain.model;

import java.time.Instant;
import java.util.UUID;

/** The game-wide declaration window opening fact for one era. */
public record DeclarationWindow(UUID gameId, int eraNumber, Instant expiresAt) {}
