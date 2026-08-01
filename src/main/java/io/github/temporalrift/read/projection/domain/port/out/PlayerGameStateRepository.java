package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.PlayerGameState;

public interface PlayerGameStateRepository {

    Optional<PlayerGameState> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    void save(PlayerGameState playerGameState);
}
