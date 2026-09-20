package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;

@Repository
class JpaPlayerSubmissionAdapter implements PlayerSubmissionRepository {

    private final PlayerSubmissionJpaRepository repository;

    JpaPlayerSubmissionAdapter(PlayerSubmissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PlayerSubmission> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return repository
                .findByGameIdAndPlayerIdAndEraNumberOrderByKindAscRoundNumberAsc(gameId, playerId, eraNumber)
                .stream()
                .map(PlayerSubmissionEntity::toDomain)
                .toList();
    }

    @Override
    public void upsert(PlayerSubmission submission) {
        var roundNumber = submission.roundNumber() == null ? 0 : submission.roundNumber();
        repository
                .findByGameIdAndPlayerIdAndEraNumberAndKindAndRoundNumber(
                        submission.gameId(),
                        submission.playerId(),
                        submission.eraNumber(),
                        submission.kind().name(),
                        roundNumber)
                .ifPresentOrElse(
                        existing -> existing.updateFrom(submission),
                        () -> repository.save(PlayerSubmissionEntity.fromDomain(UUID.randomUUID(), submission)));
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
