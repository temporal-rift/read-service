package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.port.out.ForesightPreviewRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryForesightPreviewAdapter implements ForesightPreviewRepository {

    private final Table<UUID, ForesightPreview> previews;

    InMemoryForesightPreviewAdapter(InMemoryDatabase database) {
        this.previews = database.table("foresight_preview", false);
    }

    @Override
    public Optional<ForesightPreview> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return previews.find(gameId, RowKey.of(playerId, eraNumber));
    }

    @Override
    public void upsert(ForesightPreview preview) {
        previews.put(preview.gameId(), RowKey.of(preview.playerId(), preview.eraNumber()), preview);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        previews.removeIf(gameId, preview -> preview.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        previews.removePartition(gameId);
    }
}
