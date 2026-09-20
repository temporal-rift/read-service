package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.TerminalResult;
import io.github.temporalrift.read.projection.domain.port.out.TerminalResultRepository;

@Repository
class JpaTerminalResultAdapter implements TerminalResultRepository {

    private final TerminalResultJpaRepository results;
    private final TerminalWinnerJpaRepository winners;
    private final TerminalFinalScoreJpaRepository finalScores;

    JpaTerminalResultAdapter(
            TerminalResultJpaRepository results,
            TerminalWinnerJpaRepository winners,
            TerminalFinalScoreJpaRepository finalScores) {
        this.results = results;
        this.winners = winners;
        this.finalScores = finalScores;
    }

    @Override
    public Optional<TerminalResult> findByGameId(UUID gameId) {
        return results.findById(gameId)
                .map(header -> new TerminalResult(
                        gameId,
                        header.getEndReason(),
                        winners.findByGameIdOrderByPlayerIdAsc(gameId).stream()
                                .map(winner -> new TerminalResult.TerminalWinner(
                                        winner.getPlayerId(), winner.getFaction(), winner.getWinType()))
                                .toList(),
                        finalScores.findByGameIdOrderByPlayerIdAsc(gameId).stream()
                                .map(score -> new TerminalResult.TerminalScore(
                                        score.getPlayerId(), score.getFaction(), score.getScore()))
                                .toList()));
    }

    @Override
    public void addWinners(UUID gameId, List<TerminalResult.TerminalWinner> newWinners) {
        ensureHeader(gameId);
        for (var winner : newWinners) {
            winners.findByGameIdAndPlayerId(gameId, winner.playerId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setFaction(winner.faction());
                                existing.setWinType(winner.winType());
                            },
                            () -> winners.save(new TerminalWinnerEntity(
                                    UUID.randomUUID(), gameId, winner.playerId(), winner.faction(), winner.winType())));
        }
    }

    @Override
    public void saveEndReasonAndScores(UUID gameId, String endReason, List<TerminalResult.TerminalScore> scores) {
        var header = ensureHeader(gameId);
        header.setEndReason(endReason);
        for (var score : scores) {
            finalScores
                    .findByGameIdAndPlayerId(gameId, score.playerId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setFaction(score.faction());
                                existing.setScore(score.score());
                            },
                            () -> finalScores.save(new TerminalFinalScoreEntity(
                                    UUID.randomUUID(), gameId, score.playerId(), score.faction(), score.score())));
        }
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        winners.deleteByGameId(gameId);
        finalScores.deleteByGameId(gameId);
        results.deleteById(gameId);
    }

    private TerminalResultEntity ensureHeader(UUID gameId) {
        return results.findById(gameId).orElseGet(() -> results.save(new TerminalResultEntity(gameId)));
    }
}
