package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;

@Repository
class JpaGameProjectionAdapter implements GameProjectionRepository {

    private final GameProjectionJpaRepository repository;

    JpaGameProjectionAdapter(GameProjectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<GameProjection> findByGameId(UUID gameId) {
        return repository.findById(gameId).map(GameProjectionEntity::toDomain);
    }

    @Override
    public void save(GameProjection gameProjection) {
        var existing = repository.findById(gameProjection.gameId());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setEraNumber(gameProjection.eraNumber());
            entity.setPhase(gameProjection.phase());
            entity.setPendingParadoxIds(gameProjection.pendingParadoxIds());
        } else {
            repository.save(GameProjectionEntity.fromDomain(gameProjection));
        }
    }
}
