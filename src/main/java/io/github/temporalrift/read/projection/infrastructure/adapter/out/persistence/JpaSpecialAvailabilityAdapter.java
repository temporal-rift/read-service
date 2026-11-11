package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.SpecialAvailabilityReport;
import io.github.temporalrift.read.projection.domain.port.out.SpecialAvailabilityRepository;

@Repository
@Profile("!in-memory")
class JpaSpecialAvailabilityAdapter implements SpecialAvailabilityRepository {

    private final SpecialAvailabilityJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaSpecialAvailabilityAdapter(SpecialAvailabilityJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SpecialAvailabilityReport> findByGameIdAndPlayerId(UUID gameId, UUID playerId) {
        return repository.findByGameIdAndPlayerId(gameId, playerId).map(entity -> entity.toDomain(objectMapper));
    }

    @Override
    public void save(SpecialAvailabilityReport report) {
        repository
                .findByGameIdAndPlayerId(report.gameId(), report.playerId())
                .ifPresentOrElse(
                        existing -> existing.updateFrom(report, objectMapper),
                        () -> repository.save(
                                SpecialAvailabilityEntity.fromDomain(UUID.randomUUID(), report, objectMapper)));
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteByGameId(gameId);
    }
}
