package io.github.temporalrift.read.shared.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.shared.ProcessedEventPort;

@Repository
class JpaProcessedEventAdapter implements ProcessedEventPort {

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
}
