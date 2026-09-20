package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.PublicBand;

/** Stores game-scoped public band publications, replaced wholesale per game and era. */
public interface PublicBandRepository {

    List<PublicBand> findByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void replaceAll(UUID gameId, int eraNumber, List<PublicBand> bands);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
