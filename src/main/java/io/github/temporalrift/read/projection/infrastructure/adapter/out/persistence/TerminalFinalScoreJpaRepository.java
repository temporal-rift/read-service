package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface TerminalFinalScoreJpaRepository extends JpaRepository<TerminalFinalScoreEntity, UUID> {

    List<TerminalFinalScoreEntity> findByGameIdOrderByPlayerIdAsc(UUID gameId);

    Optional<TerminalFinalScoreEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    @Transactional
    void deleteByGameId(UUID gameId);
}
