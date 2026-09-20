package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.port.out.PublicBandRepository;

@Repository
class JpaPublicBandAdapter implements PublicBandRepository {

    private final PublicBandJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaPublicBandAdapter(PublicBandJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PublicBand> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return repository.findByGameIdAndEraNumberOrderByEventIdAsc(gameId, eraNumber).stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    @Override
    public void replaceAll(UUID gameId, int eraNumber, List<PublicBand> bands) {
        // Per-event upsert, never bulk-delete-then-insert: a bulk delete does not touch rows
        // still queued in the persistence context, so a same-transaction replace would insert
        // duplicates and violate the identity constraint. Managed-entity updates/removes flush
        // in order instead.
        var remaining = new java.util.LinkedHashMap<UUID, PublicBand>();
        for (var band : bands) {
            remaining.put(band.eventId(), band);
        }
        for (var entity : repository.findByGameIdAndEraNumberOrderByEventIdAsc(gameId, eraNumber)) {
            var replacement = remaining.remove(entity.getEventId());
            if (replacement == null) {
                repository.delete(entity);
            } else {
                entity.updateFrom(replacement, objectMapper);
            }
        }
        for (var band : remaining.values()) {
            repository.save(PublicBandEntity.fromDomain(UUID.randomUUID(), band, objectMapper));
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
