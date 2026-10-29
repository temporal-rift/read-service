package io.github.temporalrift.read.shared.infrastructure.adapter.out.inmemory;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryProcessedEventAdapter implements ProcessedEventPort, GameplayState {

    private final Table<UUID, Instant> processedEvents;

    InMemoryProcessedEventAdapter(InMemoryDatabase database) {
        this.processedEvents = database.table("processed_events", false);
    }

    // The lock makes a second claimer of the same identity wait for an uncommitted claim, like a unique-index insert.
    @Override
    @Transactional
    public boolean claim(UUID eventId, String consumer) {
        var key = RowKey.of(consumer);
        processedEvents.lock(eventId, key);
        return processedEvents.putIfAbsent(eventId, key, Instant.now());
    }

    @Override
    @Transactional
    public boolean isEmpty() {
        return processedEvents.isEmpty();
    }

    @Override
    @Transactional
    public void discardAll() {
        processedEvents.clear();
    }
}
