package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GameResolvedEventJpaRepository extends JpaRepository<GameResolvedEventEntity, UUID> {

    boolean existsByGameIdAndEventId(UUID gameId, UUID eventId);

    @Modifying
    @Query(value = """
                    INSERT INTO game_resolved_event (id, game_id, event_id)
                    VALUES (:id, :gameId, :eventId)
                    ON CONFLICT (game_id, event_id) DO NOTHING
                    """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("gameId") UUID gameId, @Param("eventId") UUID eventId);
}
