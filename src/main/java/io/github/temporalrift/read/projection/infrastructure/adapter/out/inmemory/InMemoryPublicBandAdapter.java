package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.port.out.PublicBandRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.shared.inmemory.UuidOrder;

@Repository
@Profile("in-memory")
class InMemoryPublicBandAdapter implements PublicBandRepository {

    private final Table<UUID, PublicBand> bands;

    InMemoryPublicBandAdapter(InMemoryDatabase database) {
        this.bands = database.table("public_band", false);
    }

    @Override
    public List<PublicBand> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        return bands.list(gameId, band -> band.eraNumber() == eraNumber).stream()
                .sorted(Comparator.comparing(PublicBand::eventId, UuidOrder.ASCENDING))
                .toList();
    }

    @Override
    public void replaceAll(UUID gameId, int eraNumber, List<PublicBand> replacements) {
        var keptEventIds = replacements.stream().map(PublicBand::eventId).collect(Collectors.toSet());
        bands.removeIf(gameId, band -> band.eraNumber() == eraNumber && !keptEventIds.contains(band.eventId()));
        replacements.forEach(band -> bands.put(gameId, RowKey.of(eraNumber, band.eventId()), band));
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        bands.removeIf(gameId, band -> band.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        bands.removePartition(gameId);
    }
}
