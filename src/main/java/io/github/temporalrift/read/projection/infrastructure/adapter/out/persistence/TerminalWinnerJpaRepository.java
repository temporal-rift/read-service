package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface TerminalWinnerJpaRepository extends JpaRepository<TerminalWinnerEntity, UUID> {

    List<TerminalWinnerEntity> findByGameIdOrderByPlayerIdAsc(UUID gameId);

    Optional<TerminalWinnerEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    @Transactional
    void deleteByGameId(UUID gameId);
}
