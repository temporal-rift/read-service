package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;

@Repository
class JpaGameChainAdapter implements GameChainRepository {

    private final GameChainJpaRepository repository;

    JpaGameChainAdapter(GameChainJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<GameChain> findByGameId(UUID gameId) {
        return repository.findById(gameId).map(GameChainEntity::toDomain);
    }

    @Override
    public void save(UUID gameId, GameChain chain) {
        var existing = repository.findById(gameId);
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setChainId(chain.chainId());
            entity.setStatus(chain.status());
            entity.setLength(chain.length());
        } else {
            repository.save(GameChainEntity.fromDomain(chain));
        }
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteById(gameId);
    }
}
