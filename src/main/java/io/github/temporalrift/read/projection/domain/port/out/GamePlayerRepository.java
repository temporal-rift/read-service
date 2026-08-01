package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;

public interface GamePlayerRepository {

    List<GamePlayer> findByGameId(UUID gameId);

    Optional<GamePlayer> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    void save(UUID gameId, GamePlayer gamePlayer);
}
