package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryPlayerSubmissionAdapter implements PlayerSubmissionRepository {

    // Stored and ordered by the window name, with no round (null) sorting as round 0.
    private static final Comparator<PlayerSubmission> ORDER = Comparator.comparing(
                    (PlayerSubmission submission) -> submission.window().name())
            .thenComparingInt(InMemoryPlayerSubmissionAdapter::round);

    private final Table<UUID, PlayerSubmission> submissions;

    InMemoryPlayerSubmissionAdapter(InMemoryDatabase database) {
        this.submissions = database.table("player_submission", false);
    }

    @Override
    public List<PlayerSubmission> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return submissions
                .list(
                        gameId,
                        submission -> submission.playerId().equals(playerId) && submission.eraNumber() == eraNumber)
                .stream()
                .sorted(ORDER)
                .toList();
    }

    @Override
    public List<UUID> findSubmittedPlayerIds(
            UUID gameId, int eraNumber, PlayerSubmission.SubmissionWindow window, Integer roundNumber) {
        var round = roundNumber == null ? 0 : roundNumber;
        return submissions
                .list(
                        gameId,
                        submission -> submission.eraNumber() == eraNumber
                                && submission.window() == window
                                && round(submission) == round)
                .stream()
                .map(PlayerSubmission::playerId)
                .distinct()
                .toList();
    }

    @Override
    public void upsert(PlayerSubmission submission) {
        submissions.put(
                submission.gameId(),
                RowKey.of(submission.playerId(), submission.eraNumber(), submission.window(), round(submission)),
                submission);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        submissions.removeIf(gameId, submission -> submission.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        submissions.removePartition(gameId);
    }

    private static int round(PlayerSubmission submission) {
        return submission.roundNumber() == null ? 0 : submission.roundNumber();
    }
}
