package io.github.temporalrift.read.projection.domain.port.out;

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;

/** Stores player-private Scan results independently from the player-state projection lifecycle. */
public interface RevealedProbabilityIntelRepository extends RevealedIntelRepository<RevealedProbabilityIntel> {}
