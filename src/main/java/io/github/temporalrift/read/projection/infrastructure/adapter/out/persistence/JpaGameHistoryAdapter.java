package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

@Repository
class JpaGameHistoryAdapter implements GameHistoryRepository {

    private final GameHistoryProjectionJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaGameHistoryAdapter(GameHistoryProjectionJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<GameHistoryProjection> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return repository.findByGameIdAndEraNumber(gameId, eraNumber).map(entity -> entity.toDomain(objectMapper));
    }

    @Override
    public List<GameHistoryProjection> findByGameId(UUID gameId) {
        return repository.findByGameIdOrderByEraNumberAsc(gameId).stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    @Override
    public void save(GameHistoryProjection projection) {
        repository
                .findByGameIdAndEraNumber(projection.gameId(), projection.eraNumber())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(projection, objectMapper),
                        () -> repository.save(
                                GameHistoryProjectionEntity.fromDomain(UUID.randomUUID(), projection, objectMapper)));
    }
}
