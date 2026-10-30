package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;

@Repository
@Profile("!in-memory")
class JpaGameActiveEventAdapter implements GameActiveEventRepository {

    private final GameActiveEventJpaRepository repository;
    private final GameResolvedEventJpaRepository resolvedRepository;

    JpaGameActiveEventAdapter(
            GameActiveEventJpaRepository repository, GameResolvedEventJpaRepository resolvedRepository) {
        this.repository = repository;
        this.resolvedRepository = resolvedRepository;
    }

    @Override
    public List<GameActiveEvent> findByGameId(UUID gameId) {
        return repository.findByGameIdOrderByRevealIndexAscEventIdAsc(gameId).stream()
                .map(GameActiveEventEntity::toDomain)
                .toList();
    }

    @Override
    public void save(UUID gameId, GameActiveEvent gameActiveEvent) {
        // A row still held for the same event comes from an earlier draw: a carried event whose previous era was never
        // cleared. The current draw's facts replace it; a redelivered draw writes the same values again.
        var entity = repository
                .findByGameIdAndEventId(gameId, gameActiveEvent.eventId())
                .map(existing -> {
                    existing.redraw(gameActiveEvent);
                    return existing;
                })
                .orElseGet(() -> GameActiveEventEntity.fromDomain(UUID.randomUUID(), gameId, gameActiveEvent));
        repository.save(entity);
    }

    @Override
    public void deleteByGameIdAndEventId(UUID gameId, UUID eventId) {
        repository.deleteByGameIdAndEventId(gameId, eventId);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteByGameId(gameId);
    }

    @Override
    public boolean isResolved(UUID gameId, UUID eventId) {
        return resolvedRepository.existsByGameIdAndEventId(gameId, eventId);
    }

    @Override
    public void markResolved(UUID gameId, UUID eventId) {
        resolvedRepository.insertIfAbsent(UUID.randomUUID(), gameId, eventId);
    }
}
