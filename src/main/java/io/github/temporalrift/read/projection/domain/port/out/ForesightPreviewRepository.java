package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.ForesightPreview;

/** Player-private Foresight previews keyed by game, viewer, and the era they were revealed in. */
public interface ForesightPreviewRepository {

    Optional<ForesightPreview> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber);

    void upsert(ForesightPreview preview);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    /** Removes every era's previews for the game; only the terminal game end may use this. */
    void deleteByGameId(UUID gameId);
}
