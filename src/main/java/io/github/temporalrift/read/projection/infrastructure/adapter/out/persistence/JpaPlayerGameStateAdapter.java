package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;

@Repository
class JpaPlayerGameStateAdapter implements PlayerGameStateRepository {

    private final PlayerGameStateJpaRepository repository;

    JpaPlayerGameStateAdapter(PlayerGameStateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PlayerGameState> findByGameIdAndPlayerId(UUID gameId, UUID playerId) {
        return repository.findByGameIdAndPlayerId(gameId, playerId).map(PlayerGameStateEntity::toDomain);
    }

    @Override
    public void save(PlayerGameState playerGameState) {
        var existing = repository.findByGameIdAndPlayerId(playerGameState.gameId(), playerGameState.playerId());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setMyFaction(playerGameState.myFaction());
            entity.setHand(playerGameState.myHand().stream()
                    .map(PlayerGameStateHandCardValue::fromDomain)
                    .toList());
            entity.setPendingHandSelection(playerGameState.pendingHandSelection());
        } else {
            repository.save(PlayerGameStateEntity.fromDomain(UUID.randomUUID(), playerGameState));
        }
    }
}
