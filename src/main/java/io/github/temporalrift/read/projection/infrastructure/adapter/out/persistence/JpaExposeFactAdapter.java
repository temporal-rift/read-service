package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;

@Repository
class JpaExposeFactAdapter implements ExposeFactRepository {

    private final ExposeFactJpaRepository repository;

    JpaExposeFactAdapter(ExposeFactJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ExposeFact> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return repository
                .findByGameIdAndEraNumberOrderByActivistPlayerIdAscTargetPlayerIdAscRoundNumberAsc(gameId, eraNumber)
                .stream()
                .map(ExposeFactEntity::toDomain)
                .toList();
    }

    @Override
    public void upsert(ExposeFact fact) {
        repository
                .findByGameIdAndEraNumberAndActivistPlayerIdAndTargetPlayerIdAndRoundNumber(
                        fact.gameId(),
                        fact.eraNumber(),
                        fact.activistPlayerId(),
                        fact.targetPlayerId(),
                        fact.roundNumber())
                .ifPresentOrElse(
                        existing -> existing.updateFrom(fact),
                        () -> repository.save(ExposeFactEntity.fromDomain(UUID.randomUUID(), fact)));
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
