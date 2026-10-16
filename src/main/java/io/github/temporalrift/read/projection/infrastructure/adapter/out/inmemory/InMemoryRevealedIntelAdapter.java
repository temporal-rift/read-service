package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
import io.github.temporalrift.read.projection.domain.port.out.RevealedIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.shared.inmemory.UuidOrder;

/** Latest-round-wins storage shared by the per-kind revealed-intel adapters. */
abstract class InMemoryRevealedIntelAdapter<D extends RevealedIntelEntry> implements RevealedIntelRepository<D> {

    private final Table<UUID, D> intel;

    InMemoryRevealedIntelAdapter(InMemoryDatabase database, String tableName) {
        this.intel = database.table(tableName, false);
    }

    @Override
    public List<D> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return intel.list(gameId, entry -> entry.playerId().equals(playerId) && entry.eraNumber() == eraNumber).stream()
                .sorted(Comparator.comparing(RevealedIntelEntry::eventId, UuidOrder.ASCENDING))
                .toList();
    }

    @Override
    public void upsertLatest(D latest) {
        var key = RowKey.of(latest.playerId(), latest.eraNumber(), latest.eventId());
        var existing = intel.find(latest.gameId(), key);
        if (existing.isEmpty() || latest.observedInRound() >= existing.get().observedInRound()) {
            intel.put(latest.gameId(), key, latest);
        }
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        intel.removeIf(gameId, entry -> entry.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        intel.removePartition(gameId);
    }
}
