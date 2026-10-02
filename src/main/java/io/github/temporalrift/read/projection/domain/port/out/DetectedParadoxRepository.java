package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.DetectedParadox;

/** Stores the public detail of each detected paradox; a recorded paradox's detail never changes. */
public interface DetectedParadoxRepository {
    List<DetectedParadox> findByGameIdAndParadoxIds(UUID gameId, Collection<UUID> paradoxIds);

    void saveIfAbsent(DetectedParadox paradox);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
