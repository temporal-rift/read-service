package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryGameHistoryAdapter implements GameHistoryRepository {

    private final Table<UUID, GameHistoryProjection> histories;

    InMemoryGameHistoryAdapter(InMemoryDatabase database) {
        this.histories = database.table("game_history_projection", true);
    }

    @Override
    public Optional<GameHistoryProjection> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return histories.find(gameId, RowKey.of(eraNumber));
    }

    @Override
    public List<GameHistoryProjection> findByGameId(UUID gameId) {
        return histories.list(gameId).stream()
                .sorted(Comparator.comparingInt(GameHistoryProjection::eraNumber))
                .toList();
    }

    @Override
    public void save(GameHistoryProjection projection) {
        histories.put(projection.gameId(), RowKey.of(projection.eraNumber()), projection);
    }
}
