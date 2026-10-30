package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface GameActiveEventJpaRepository extends JpaRepository<GameActiveEventEntity, UUID> {

    /** Reveal order; the event id only orders rows stored before reveal positions were recorded. */
    List<GameActiveEventEntity> findByGameIdOrderByRevealIndexAscEventIdAsc(UUID gameId);

    Optional<GameActiveEventEntity> findByGameIdAndEventId(UUID gameId, UUID eventId);

    @Transactional
    void deleteByGameIdAndEventId(UUID gameId, UUID eventId);

    @Transactional
    void deleteByGameId(UUID gameId);
}
