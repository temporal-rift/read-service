package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryGameProjectionAdapter implements GameProjectionRepository {

    private static final RowKey PROJECTION = RowKey.of("projection");

    private final Table<UUID, GameProjection> projections;

    InMemoryGameProjectionAdapter(InMemoryDatabase database) {
        this.projections = database.table("game_projection", false);
    }

    @Override
    public Optional<GameProjection> findByGameId(UUID gameId) {
        return projections.find(gameId, PROJECTION);
    }

    @Override
    public Optional<GameProjection> findByGameIdForUpdate(UUID gameId) {
        projections.lock(gameId, PROJECTION);
        projections.putIfAbsent(gameId, PROJECTION, anchor(gameId));
        return projections.find(gameId, PROJECTION);
    }

    @Override
    public void save(GameProjection gameProjection) {
        var gameId = gameProjection.gameId();
        var saved = projections
                .find(gameId, PROJECTION)
                .map(existing -> withFreshness(gameProjection, existing))
                .orElseGet(() -> withFreshness(gameProjection, 0, gameProjection.winScoreThreshold()));
        projections.put(gameId, PROJECTION, saved);
    }

    private static GameProjection anchor(UUID gameId) {
        return new GameProjection(gameId, 0, Phase.LOBBY, List.of(), null, null, null, null, 0, null, List.of(), null);
    }

    // The threshold is set once by GameStarted: a later save without one keeps it.
    private static GameProjection withFreshness(GameProjection incoming, GameProjection existing) {
        var threshold =
                incoming.winScoreThreshold() != null ? incoming.winScoreThreshold() : existing.winScoreThreshold();
        return withFreshness(incoming, existing.revision() + 1, threshold);
    }

    private static GameProjection withFreshness(GameProjection incoming, long revision, Integer threshold) {
        return new GameProjection(
                incoming.gameId(),
                incoming.eraNumber(),
                incoming.phase(),
                incoming.pendingParadoxIds(),
                incoming.lastRoundSummary(),
                incoming.currentRoundNumber(),
                incoming.actionRoundExpiresAt(),
                incoming.paradoxResolutionExpiresAt(),
                revision,
                Instant.now(),
                incoming.affectedEventIds(),
                threshold);
    }
}
