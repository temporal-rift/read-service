package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;

@Repository
class JpaGameActiveEventAdapter implements GameActiveEventRepository {

    private final GameActiveEventJpaRepository repository;

    JpaGameActiveEventAdapter(GameActiveEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<GameActiveEvent> findByGameId(UUID gameId) {
        return repository.findByGameId(gameId).stream()
                .map(GameActiveEventEntity::toDomain)
                .toList();
    }

    @Override
    public void save(UUID gameId, GameActiveEvent gameActiveEvent) {
        // Active events are immutable once drawn in this slice (no probability updates here) — a duplicate
        // EventsDrawn delivery (shouldn't happen given upstream idempotency, but defensively) is a no-op.
        if (repository.findByGameIdAndEventId(gameId, gameActiveEvent.eventId()).isEmpty()) {
            repository.save(GameActiveEventEntity.fromDomain(UUID.randomUUID(), gameId, gameActiveEvent));
        }
    }

    @Override
    public void deleteByGameIdAndEventId(UUID gameId, UUID eventId) {
        repository.deleteByGameIdAndEventId(gameId, eventId);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteByGameId(gameId);
    }
}
