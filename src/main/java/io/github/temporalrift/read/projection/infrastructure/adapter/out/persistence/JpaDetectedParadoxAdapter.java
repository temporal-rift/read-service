package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.DetectedParadox;
import io.github.temporalrift.read.projection.domain.port.out.DetectedParadoxRepository;

@Repository
class JpaDetectedParadoxAdapter implements DetectedParadoxRepository {
    private final DetectedParadoxJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaDetectedParadoxAdapter(DetectedParadoxJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<DetectedParadox> findByGameIdAndParadoxIds(UUID gameId, Collection<UUID> paradoxIds) {
        if (paradoxIds.isEmpty()) {
            return List.of();
        }
        return repository.findByGameIdAndParadoxIdIn(gameId, paradoxIds).stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    @Override
    public void saveIfAbsent(DetectedParadox paradox) {
        if (!repository.existsById(paradox.paradoxId())) {
            repository.save(DetectedParadoxEntity.fromDomain(paradox, objectMapper));
        }
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
