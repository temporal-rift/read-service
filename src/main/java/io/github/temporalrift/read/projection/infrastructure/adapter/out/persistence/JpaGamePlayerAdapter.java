package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;

@Repository
class JpaGamePlayerAdapter implements GamePlayerRepository {

    private final GamePlayerJpaRepository repository;

    JpaGamePlayerAdapter(GamePlayerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<GamePlayer> findByGameId(UUID gameId) {
        return repository.findByGameId(gameId).stream()
                .map(GamePlayerEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<GamePlayer> findByGameIdAndPlayerId(UUID gameId, UUID playerId) {
        return repository.findByGameIdAndPlayerId(gameId, playerId).map(GamePlayerEntity::toDomain);
    }

    @Override
    public void save(UUID gameId, GamePlayer gamePlayer) {
        var existing = repository.findByGameIdAndPlayerId(gameId, gamePlayer.playerId());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setScore(gamePlayer.score());
            entity.setConnected(gamePlayer.isConnected());
            entity.setFaction(gamePlayer.faction());
        } else {
            repository.save(GamePlayerEntity.fromDomain(UUID.randomUUID(), gameId, gamePlayer));
        }
    }
}
