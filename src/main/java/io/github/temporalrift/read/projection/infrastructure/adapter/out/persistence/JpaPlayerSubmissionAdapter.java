package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;

@Repository
class JpaPlayerSubmissionAdapter implements PlayerSubmissionRepository {

    private final PlayerSubmissionJpaRepository repository;
    private final ObjectMapper objectMapper;

    JpaPlayerSubmissionAdapter(PlayerSubmissionJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PlayerSubmission> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return repository
                .findByGameIdAndPlayerIdAndEraNumberOrderBySubmissionWindowAscRoundNumberAsc(
                        gameId, playerId, eraNumber)
                .stream()
                .map(entity -> entity.toDomain(objectMapper))
                .toList();
    }

    @Override
    public List<UUID> findSubmittedPlayerIds(
            UUID gameId, int eraNumber, PlayerSubmission.SubmissionWindow window, Integer roundNumber) {
        return repository.findSubmittedPlayerIds(
                gameId, eraNumber, window.name(), roundNumber == null ? 0 : roundNumber);
    }

    @Override
    public void upsert(PlayerSubmission submission) {
        var roundNumber = submission.roundNumber() == null ? 0 : submission.roundNumber();
        repository
                .findByGameIdAndPlayerIdAndEraNumberAndSubmissionWindowAndRoundNumber(
                        submission.gameId(),
                        submission.playerId(),
                        submission.eraNumber(),
                        submission.window().name(),
                        roundNumber)
                .ifPresentOrElse(
                        existing -> existing.updateFrom(submission, objectMapper),
                        () -> repository.save(
                                PlayerSubmissionEntity.fromDomain(UUID.randomUUID(), submission, objectMapper)));
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
