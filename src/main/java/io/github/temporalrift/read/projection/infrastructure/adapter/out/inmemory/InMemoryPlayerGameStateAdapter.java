package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryPlayerGameStateAdapter implements PlayerGameStateRepository {

    private final Table<UUID, PlayerGameState> states;

    InMemoryPlayerGameStateAdapter(InMemoryDatabase database) {
        this.states = database.table("player_game_state", false);
    }

    @Override
    public Optional<PlayerGameState> findByGameIdAndPlayerId(UUID gameId, UUID playerId) {
        return states.find(gameId, RowKey.of(playerId));
    }

    @Override
    public void save(PlayerGameState playerGameState) {
        states.put(playerGameState.gameId(), RowKey.of(playerGameState.playerId()), playerGameState);
    }
}
