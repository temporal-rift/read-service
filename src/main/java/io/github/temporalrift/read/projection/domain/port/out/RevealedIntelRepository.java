package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;

/** Player-private current-era intel keyed by game, viewer, era, and event. */
public interface RevealedIntelRepository<D extends RevealedIntelEntry> {

    List<D> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber);

    void upsertLatest(D intel);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    /**
     * Removes every row for the game across all eras. Only the terminal game end may use
     * this: era ends clear only their own era, since intel for other eras must survive.
     */
    void deleteByGameId(UUID gameId);
}
