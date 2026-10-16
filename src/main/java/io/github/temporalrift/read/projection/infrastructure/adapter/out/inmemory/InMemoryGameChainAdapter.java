package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryGameChainAdapter implements GameChainRepository {

    private static final RowKey CHAIN = RowKey.of("chain");

    private final Table<UUID, GameChain> chains;

    InMemoryGameChainAdapter(InMemoryDatabase database) {
        this.chains = database.table("game_chain", false);
    }

    @Override
    public Optional<GameChain> findByGameId(UUID gameId) {
        return chains.find(gameId, CHAIN);
    }

    @Override
    public void save(UUID gameId, GameChain chain) {
        chains.put(gameId, CHAIN, chain);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        chains.removePartition(gameId);
    }
}
