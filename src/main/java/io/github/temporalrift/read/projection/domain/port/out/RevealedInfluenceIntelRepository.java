package io.github.temporalrift.read.projection.domain.port.out;

import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;

/** Stores player-private Trace results independently from the player-state projection lifecycle. */
public interface RevealedInfluenceIntelRepository extends RevealedIntelRepository<RevealedInfluenceIntel> {}
