package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface GamePlayerJpaRepository extends JpaRepository<GamePlayerEntity, UUID> {

    List<GamePlayerEntity> findByGameId(UUID gameId);

    Optional<GamePlayerEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);
}
