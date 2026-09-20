package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.TerminalResult;

/** Stores the authoritative terminal facts of an ended game, assembled incrementally. */
public interface TerminalResultRepository {

    Optional<TerminalResult> findByGameId(UUID gameId);

    void addWinners(UUID gameId, List<TerminalResult.TerminalWinner> winners);

    void saveEndReasonAndScores(UUID gameId, String endReason, List<TerminalResult.TerminalScore> finalScores);

    void deleteByGameId(UUID gameId);
}
