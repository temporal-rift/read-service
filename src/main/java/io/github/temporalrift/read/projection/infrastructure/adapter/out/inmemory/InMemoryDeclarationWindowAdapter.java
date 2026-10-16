package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationWindowRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryDeclarationWindowAdapter implements DeclarationWindowRepository {

    private final Table<UUID, DeclarationWindow> windows;

    InMemoryDeclarationWindowAdapter(InMemoryDatabase database) {
        this.windows = database.table("declaration_window", false);
    }

    @Override
    public Optional<DeclarationWindow> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return windows.find(gameId, RowKey.of(eraNumber));
    }

    @Override
    public void saveIfAbsent(DeclarationWindow window) {
        windows.putIfAbsent(window.gameId(), RowKey.of(window.eraNumber()), window);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        windows.removeIf(gameId, window -> window.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        windows.removePartition(gameId);
    }
}
