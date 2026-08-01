package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface GameActiveEventJpaRepository extends JpaRepository<GameActiveEventEntity, UUID> {

    List<GameActiveEventEntity> findByGameId(UUID gameId);

    Optional<GameActiveEventEntity> findByGameIdAndEventId(UUID gameId, UUID eventId);

    @Transactional
    void deleteByGameIdAndEventId(UUID gameId, UUID eventId);

    @Transactional
    void deleteByGameId(UUID gameId);
}
