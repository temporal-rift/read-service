package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.TerminalResult;
import io.github.temporalrift.read.projection.domain.port.out.TerminalResultRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.shared.inmemory.UuidOrder;

@Repository
@Profile("in-memory")
class InMemoryTerminalResultAdapter implements TerminalResultRepository {

    private static final RowKey HEADER = RowKey.of("header");

    private final Table<UUID, Header> headers;
    private final Table<UUID, TerminalResult.TerminalWinner> winners;
    private final Table<UUID, TerminalResult.TerminalScore> finalScores;

    InMemoryTerminalResultAdapter(InMemoryDatabase database) {
        this.headers = database.table("game_terminal_result", false);
        this.winners = database.table("game_terminal_winner", false);
        this.finalScores = database.table("game_terminal_final_score", false);
    }

    @Override
    public Optional<TerminalResult> findByGameId(UUID gameId) {
        return headers.find(gameId, HEADER)
                .map(header -> new TerminalResult(
                        gameId,
                        header.endReason(),
                        winners.list(gameId).stream()
                                .sorted(Comparator.comparing(
                                        TerminalResult.TerminalWinner::playerId, UuidOrder.ASCENDING))
                                .toList(),
                        finalScores.list(gameId).stream()
                                .sorted(Comparator.comparing(
                                        TerminalResult.TerminalScore::playerId, UuidOrder.ASCENDING))
                                .toList()));
    }

    @Override
    public void addWinners(UUID gameId, List<TerminalResult.TerminalWinner> newWinners) {
        headers.putIfAbsent(gameId, HEADER, new Header(null));
        newWinners.forEach(winner -> winners.put(gameId, RowKey.of(winner.playerId()), winner));
    }

    @Override
    public void saveEndReasonAndScores(UUID gameId, String endReason, List<TerminalResult.TerminalScore> scores) {
        headers.put(gameId, HEADER, new Header(endReason));
        scores.forEach(score -> finalScores.put(gameId, RowKey.of(score.playerId()), score));
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        winners.removePartition(gameId);
        finalScores.removePartition(gameId);
        headers.removePartition(gameId);
    }

    private record Header(String endReason) {}
}
