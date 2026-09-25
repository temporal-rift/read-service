package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.port.out.PlayerNameRepository;

@Repository
class JpaPlayerNameAdapter implements PlayerNameRepository {

    private final PlayerNameJpaRepository repository;

    JpaPlayerNameAdapter(PlayerNameJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void upsert(UUID gameId, UUID playerId, String playerName) {
        repository
                .findByGameIdAndPlayerId(gameId, playerId)
                .ifPresentOrElse(
                        existing -> existing.setPlayerName(playerName),
                        () -> repository.save(new PlayerNameEntity(UUID.randomUUID(), gameId, playerId, playerName)));
    }

    @Override
    public Map<UUID, String> findByGameId(UUID gameId) {
        return repository.findByGameId(gameId).stream()
                .collect(Collectors.toUnmodifiableMap(PlayerNameEntity::playerId, PlayerNameEntity::playerName));
    }
}
