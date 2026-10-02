package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationWindowRepository;

@Repository
class JpaDeclarationWindowAdapter implements DeclarationWindowRepository {

    private final DeclarationWindowJpaRepository repository;

    JpaDeclarationWindowAdapter(DeclarationWindowJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<DeclarationWindow> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return repository.findByGameIdAndEraNumber(gameId, eraNumber).map(DeclarationWindowEntity::toDomain);
    }

    @Override
    public void saveIfAbsent(DeclarationWindow window) {
        if (repository
                .findByGameIdAndEraNumber(window.gameId(), window.eraNumber())
                .isEmpty()) {
            repository.save(DeclarationWindowEntity.fromDomain(UUID.randomUUID(), window));
        }
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        repository.deleteByGameIdAndEraNumber(gameId, eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteByGameId(gameId);
    }
}
