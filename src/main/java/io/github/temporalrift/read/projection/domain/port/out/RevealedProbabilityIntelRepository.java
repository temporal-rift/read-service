package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;

/** Stores player-private Scan results independently from the player-state projection lifecycle. */
public interface RevealedProbabilityIntelRepository {

    List<RevealedProbabilityIntel> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber);

    void upsertLatest(RevealedProbabilityIntel intel);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);
}
