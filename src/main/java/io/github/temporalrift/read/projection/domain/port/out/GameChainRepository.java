package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameChain;

public interface GameChainRepository {

    Optional<GameChain> findByGameId(UUID gameId);

    void save(UUID gameId, GameChain chain);

    void deleteByGameId(UUID gameId);
}
