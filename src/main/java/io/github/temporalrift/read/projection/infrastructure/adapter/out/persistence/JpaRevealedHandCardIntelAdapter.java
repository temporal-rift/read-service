package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;

@Repository
class JpaRevealedHandCardIntelAdapter implements RevealedHandCardIntelRepository {

    private final RevealedHandCardIntelJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaRevealedHandCardIntelAdapter(RevealedHandCardIntelJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RevealedHandCardIntel> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return repository.findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(gameId, playerId, eraNumber).stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    @Override
    public void upsertLatest(RevealedHandCardIntel intel) {
        RevealedIntelUpsert.upsertLatest(
                intel,
                current -> repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        current.gameId(), current.playerId(), current.eraNumber(), current.eventId()),
                (existing, latest) -> existing.updateFrom(latest, objectMapper),
                () -> repository.save(RevealedHandCardIntelEntity.fromDomain(UUID.randomUUID(), intel, objectMapper)));
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        repository.deleteByGameIdAndEraNumber(gameId, eraNumber);
    }
}
