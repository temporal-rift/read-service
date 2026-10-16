package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.shared.inmemory.UuidOrder;

@Repository
@Profile("in-memory")
class InMemoryExposeFactAdapter implements ExposeFactRepository {

    private static final Comparator<ExposeFact> ORDER = Comparator.comparing(
                    ExposeFact::activistPlayerId, UuidOrder.ASCENDING)
            .thenComparing(ExposeFact::targetPlayerId, UuidOrder.ASCENDING)
            .thenComparingInt(ExposeFact::roundNumber);

    private final Table<UUID, ExposeFact> facts;

    InMemoryExposeFactAdapter(InMemoryDatabase database) {
        this.facts = database.table("expose_fact", false);
    }

    @Override
    public List<ExposeFact> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return facts.list(gameId, fact -> fact.eraNumber() == eraNumber).stream()
                .sorted(ORDER)
                .toList();
    }

    @Override
    public void upsert(ExposeFact fact) {
        facts.put(
                fact.gameId(),
                RowKey.of(fact.eraNumber(), fact.activistPlayerId(), fact.targetPlayerId(), fact.roundNumber()),
                fact);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        facts.removeIf(gameId, fact -> fact.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        facts.removePartition(gameId);
    }
}
