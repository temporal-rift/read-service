package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;
import io.github.temporalrift.read.projection.domain.port.out.PublicDeclarationRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.shared.inmemory.UuidOrder;

@Repository
@Profile("in-memory")
class InMemoryPublicDeclarationAdapter implements PublicDeclarationRepository {

    private final Table<UUID, PublicDeclaration> declarations;

    InMemoryPublicDeclarationAdapter(InMemoryDatabase database) {
        this.declarations = database.table("public_declaration", false);
    }

    @Override
    public List<PublicDeclaration> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return declarations.list(gameId, declaration -> declaration.eraNumber() == eraNumber).stream()
                .sorted(Comparator.comparing(PublicDeclaration::playerId, UuidOrder.ASCENDING))
                .toList();
    }

    @Override
    public void upsert(PublicDeclaration declaration) {
        declarations.put(declaration.gameId(), RowKey.of(declaration.eraNumber(), declaration.playerId()), declaration);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        declarations.removeIf(gameId, declaration -> declaration.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        declarations.removePartition(gameId);
    }
}
