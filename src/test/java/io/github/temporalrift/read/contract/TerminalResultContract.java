package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.TerminalResult;
import io.github.temporalrift.read.projection.domain.model.TerminalResult.TerminalScore;
import io.github.temporalrift.read.projection.domain.model.TerminalResult.TerminalWinner;
import io.github.temporalrift.read.projection.domain.port.out.TerminalResultRepository;

public abstract class TerminalResultContract extends PortContract {

    protected abstract TerminalResultRepository port();

    @Test
    void aGameWithoutTerminalFactsHasNoResult() {
        assertThat(inTransaction(() -> port().findByGameId(gameId))).isEmpty();
    }

    @Test
    void winnersAloneCreateAResultWithoutEndReasonOrScores() {
        var winner = new TerminalWinner(LOW, "ERASERS", "SCORE");

        inTransaction(() -> port().addWinners(gameId, List.of(winner)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new TerminalResult(gameId, null, List.of(winner), List.of()));
    }

    @Test
    void theResultIsAssembledIncrementallyAndOrderedByPlayer() {
        var highWinner = new TerminalWinner(HIGH, "WEAVERS", null);
        var lowWinner = new TerminalWinner(LOW, "ERASERS", "SCORE");
        var highScore = new TerminalScore(HIGH, "WEAVERS", 3);
        var lowScore = new TerminalScore(LOW, "ERASERS", 9);

        inTransaction(() -> port().addWinners(gameId, List.of(highWinner, lowWinner)));
        inTransaction(() -> port().saveEndReasonAndScores(gameId, "WIN_CONDITION_MET", List.of(highScore, lowScore)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new TerminalResult(
                        gameId, "WIN_CONDITION_MET", List.of(lowWinner, highWinner), List.of(lowScore, highScore)));
    }

    @Test
    void scoresAndEndReasonMayArriveBeforeWinners() {
        var score = new TerminalScore(LOW, "ERASERS", 9);
        inTransaction(() -> port().saveEndReasonAndScores(gameId, "TIMELINE_COLLAPSED", List.of(score)));

        var winner = new TerminalWinner(LOW, "ERASERS", null);
        inTransaction(() -> port().addWinners(gameId, List.of(winner)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new TerminalResult(gameId, "TIMELINE_COLLAPSED", List.of(winner), List.of(score)));
    }

    @Test
    void repeatingAWinnerOrScoreUpdatesItInPlace() {
        inTransaction(() -> {
            port().addWinners(gameId, List.of(new TerminalWinner(LOW, "ERASERS", null)));
            port().saveEndReasonAndScores(gameId, "A", List.of(new TerminalScore(LOW, "ERASERS", 1)));
        });

        var winner = new TerminalWinner(LOW, "ERASERS", "SCORE");
        var score = new TerminalScore(LOW, "ERASERS", 5);
        inTransaction(() -> {
            port().addWinners(gameId, List.of(winner));
            port().saveEndReasonAndScores(gameId, "B", List.of(score));
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new TerminalResult(gameId, "B", List.of(winner), List.of(score)));
    }

    @Test
    void deletingAGameRemovesItsResultOnly() {
        inTransaction(() -> {
            port().addWinners(gameId, List.of(new TerminalWinner(LOW, "ERASERS", null)));
            port().addWinners(otherGameId, List.of(new TerminalWinner(LOW, "ERASERS", null)));
        });

        inTransaction(() -> port().deleteByGameId(gameId));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).isEmpty();
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isPresent();
    }
}
