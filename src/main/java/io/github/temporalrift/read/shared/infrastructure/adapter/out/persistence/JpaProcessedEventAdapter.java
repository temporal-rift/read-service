package io.github.temporalrift.read.shared.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.ProcessedEventPort;

@Repository
@Profile("!in-memory")
class JpaProcessedEventAdapter implements ProcessedEventPort, GameplayState {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean claim(UUID eventId, String consumer) {
        var rowsInserted = entityManager
                .createNativeQuery("""
                        INSERT INTO processed_events (event_id, consumer, processed_at)
                        VALUES (:eventId, :consumer, CURRENT_TIMESTAMP)
                        ON CONFLICT (event_id, consumer) DO NOTHING
                        """)
                .setParameter("eventId", eventId)
                .setParameter("consumer", consumer)
                .executeUpdate();
        return rowsInserted == 1;
    }

    @Override
    @Transactional
    public boolean isEmpty() {
        return !Boolean.TRUE.equals(entityManager
                .createNativeQuery("SELECT EXISTS (SELECT 1 FROM processed_events)", Boolean.class)
                .getSingleResult());
    }

    @Override
    @Transactional
    public void discardAll() {
        entityManager.createNativeQuery("DELETE FROM processed_events").executeUpdate();
    }
}
