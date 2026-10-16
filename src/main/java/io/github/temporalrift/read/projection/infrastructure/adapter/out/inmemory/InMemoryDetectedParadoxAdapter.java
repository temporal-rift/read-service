package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.DetectedParadox;
import io.github.temporalrift.read.projection.domain.port.out.DetectedParadoxRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryDetectedParadoxAdapter implements DetectedParadoxRepository {

    private final Table<UUID, DetectedParadox> paradoxes;

    InMemoryDetectedParadoxAdapter(InMemoryDatabase database) {
        this.paradoxes = database.table("detected_paradox", false);
    }

    @Override
    public List<DetectedParadox> findByGameIdAndParadoxIds(UUID gameId, Collection<UUID> paradoxIds) {
        return paradoxes.list(gameId, paradox -> paradoxIds.contains(paradox.paradoxId()));
    }

    @Override
    public void saveIfAbsent(DetectedParadox paradox) {
        paradoxes.putIfAbsent(paradox.gameId(), RowKey.of(paradox.paradoxId()), paradox);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        paradoxes.removeIf(gameId, paradox -> paradox.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        paradoxes.removePartition(gameId);
    }
}
