package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlayerGameStateJpaRepository extends JpaRepository<PlayerGameStateEntity, UUID> {

    Optional<PlayerGameStateEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);
}
