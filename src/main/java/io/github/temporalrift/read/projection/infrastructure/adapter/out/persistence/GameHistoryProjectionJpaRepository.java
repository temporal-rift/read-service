package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface GameHistoryProjectionJpaRepository extends JpaRepository<GameHistoryProjectionEntity, UUID> {

    Optional<GameHistoryProjectionEntity> findByGameIdAndEraNumber(UUID gameId, int eraNumber);

    List<GameHistoryProjectionEntity> findByGameIdOrderByEraNumberAsc(UUID gameId);
}
