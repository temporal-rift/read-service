package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;

@Repository
class JpaRevealedProbabilityIntelAdapter implements RevealedProbabilityIntelRepository {

    private final RevealedProbabilityIntelJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaRevealedProbabilityIntelAdapter(RevealedProbabilityIntelJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RevealedProbabilityIntel> findByGameIdAndPlayerIdAndEraNumber(
            UUID gameId, UUID playerId, int eraNumber) {
        return repository.findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(gameId, playerId, eraNumber).stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    @Override
    public void upsertLatest(RevealedProbabilityIntel intel) {
        RevealedIntelUpsert.upsertLatest(
                intel,
                current -> repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        current.gameId(), current.playerId(), current.eraNumber(), current.eventId()),
                (existing, latest) -> existing.updateFrom(latest, objectMapper),
                () -> repository.save(
                        RevealedProbabilityIntelEntity.fromDomain(UUID.randomUUID(), intel, objectMapper)));
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
