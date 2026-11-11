package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.SpecialAvailabilityReport;
import io.github.temporalrift.read.projection.domain.port.out.SpecialAvailabilityRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemorySpecialAvailabilityAdapter implements SpecialAvailabilityRepository {

    private final Table<UUID, SpecialAvailabilityReport> reports;

    InMemorySpecialAvailabilityAdapter(InMemoryDatabase database) {
        this.reports = database.table("special_action_availability", false);
    }

    @Override
    public Optional<SpecialAvailabilityReport> findByGameIdAndPlayerId(UUID gameId, UUID playerId) {
        return reports.find(gameId, RowKey.of(playerId));
    }

    @Override
    public void save(SpecialAvailabilityReport report) {
        reports.put(report.gameId(), RowKey.of(report.playerId()), report);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        reports.removePartition(gameId);
    }
}
