package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryGamePlayerAdapter implements GamePlayerRepository {

    private final Table<UUID, GamePlayer> players;

    InMemoryGamePlayerAdapter(InMemoryDatabase database) {
        this.players = database.table("game_player", false);
    }

    @Override
    public List<GamePlayer> findByGameId(UUID gameId) {
        return players.list(gameId);
    }

    @Override
    public Optional<GamePlayer> findByGameIdAndPlayerId(UUID gameId, UUID playerId) {
        return players.find(gameId, RowKey.of(playerId));
    }

    @Override
    public void save(UUID gameId, GamePlayer gamePlayer) {
        players.put(gameId, RowKey.of(gamePlayer.playerId()), gamePlayer);
    }
}
