package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;
import io.github.temporalrift.read.projection.domain.port.out.PublicDeclarationRepository;

@Repository
class JpaPublicDeclarationAdapter implements PublicDeclarationRepository {

    private final PublicDeclarationJpaRepository repository;

    JpaPublicDeclarationAdapter(PublicDeclarationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PublicDeclaration> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return repository.findByGameIdAndEraNumberOrderByPlayerIdAsc(gameId, eraNumber).stream()
                .map(PublicDeclarationEntity::toDomain)
                .toList();
    }

    @Override
    public void upsert(PublicDeclaration declaration) {
        repository
                .findByGameIdAndEraNumberAndPlayerId(
                        declaration.gameId(), declaration.eraNumber(), declaration.playerId())
                .ifPresentOrElse(
                        existing -> existing.updateFrom(declaration),
                        () -> repository.save(PublicDeclarationEntity.fromDomain(UUID.randomUUID(), declaration)));
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
