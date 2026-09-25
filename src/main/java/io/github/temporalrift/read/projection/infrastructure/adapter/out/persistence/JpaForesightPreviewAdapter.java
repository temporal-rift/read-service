package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.port.out.ForesightPreviewRepository;

@Repository
class JpaForesightPreviewAdapter implements ForesightPreviewRepository {

    private final ForesightPreviewJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaForesightPreviewAdapter(ForesightPreviewJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ForesightPreview> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return repository
                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber)
                .map(entity -> entity.toDomain(objectMapper));
    }

    @Override
    public void upsert(ForesightPreview preview) {
        repository
                .findByGameIdAndPlayerIdAndEraNumber(preview.gameId(), preview.playerId(), preview.eraNumber())
                .ifPresentOrElse(
                        existing -> existing.updateFrom(preview, objectMapper),
                        () -> repository.save(
                                ForesightPreviewEntity.fromDomain(UUID.randomUUID(), preview, objectMapper)));
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        repository.deleteByGameIdAndEraNumber(gameId, eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteByGameId(gameId);
    }
}
