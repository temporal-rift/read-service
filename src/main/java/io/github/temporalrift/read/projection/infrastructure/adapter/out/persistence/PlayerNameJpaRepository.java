package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlayerNameJpaRepository extends JpaRepository<PlayerNameEntity, UUID> {

    List<PlayerNameEntity> findByGameId(UUID gameId);

    Optional<PlayerNameEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);
}
