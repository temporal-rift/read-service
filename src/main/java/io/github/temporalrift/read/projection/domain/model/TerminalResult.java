package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** Authoritative terminal facts for an ended game, assembled from the owner's end-of-game publications. */
public record TerminalResult(
        UUID gameId, String endReason, List<TerminalWinner> winners, List<TerminalScore> finalScores) {

    public TerminalResult {
        winners = List.copyOf(winners);
        finalScores = List.copyOf(finalScores);
    }

    public record TerminalWinner(UUID playerId, String faction, String winType) {

        public TerminalWinner(UUID playerId, String faction) {
            this(playerId, faction, null);
        }
    }

    public record TerminalScore(UUID playerId, String faction, int score) {}
}
