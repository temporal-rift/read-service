package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.ExposeFact;

/** Stores game-scoped public Expose outcomes, keyed by activist, target and round. */
public interface ExposeFactRepository {

    List<ExposeFact> findByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void upsert(ExposeFact fact);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
