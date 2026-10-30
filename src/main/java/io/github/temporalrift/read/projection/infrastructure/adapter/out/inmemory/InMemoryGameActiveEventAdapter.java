package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.shared.inmemory.UuidOrder;

@Repository
@Profile("in-memory")
class InMemoryGameActiveEventAdapter implements GameActiveEventRepository {

    private static final Comparator<GameActiveEvent> REVEAL_ORDER = Comparator.comparingInt(
                    GameActiveEvent::revealIndex)
            .thenComparing(GameActiveEvent::eventId, UuidOrder.ASCENDING);

    private final Table<UUID, GameActiveEvent> activeEvents;
    private final Table<UUID, UUID> resolvedEvents;

    InMemoryGameActiveEventAdapter(InMemoryDatabase database) {
        this.activeEvents = database.table("game_active_event", false);
        this.resolvedEvents = database.table("game_resolved_event", false);
    }

    @Override
    public List<GameActiveEvent> findByGameId(UUID gameId) {
        return activeEvents.list(gameId).stream().sorted(REVEAL_ORDER).toList();
    }

    @Override
    public void save(UUID gameId, GameActiveEvent gameActiveEvent) {
        activeEvents.put(gameId, RowKey.of(gameActiveEvent.eventId()), gameActiveEvent);
    }

    @Override
    public void deleteByGameIdAndEventId(UUID gameId, UUID eventId) {
        activeEvents.remove(gameId, RowKey.of(eventId));
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        activeEvents.removePartition(gameId);
    }

    @Override
    public boolean isResolved(UUID gameId, UUID eventId) {
        return resolvedEvents.find(gameId, RowKey.of(eventId)).isPresent();
    }

    @Override
    public void markResolved(UUID gameId, UUID eventId) {
        resolvedEvents.putIfAbsent(gameId, RowKey.of(eventId), eventId);
    }
}
