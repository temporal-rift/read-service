package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameProjection;

public interface GameProjectionRepository {

    Optional<GameProjection> findByGameId(UUID gameId);

    void save(GameProjection gameProjection);
}
