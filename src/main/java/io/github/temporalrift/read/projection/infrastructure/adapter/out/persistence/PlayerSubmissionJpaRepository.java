package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

interface PlayerSubmissionJpaRepository extends JpaRepository<PlayerSubmissionEntity, UUID> {

    List<PlayerSubmissionEntity> findByGameIdAndPlayerIdAndEraNumberOrderBySubmissionWindowAscRoundNumberAsc(
            UUID gameId, UUID playerId, int eraNumber);

    Optional<PlayerSubmissionEntity> findByGameIdAndPlayerIdAndEraNumberAndSubmissionWindowAndRoundNumber(
            UUID gameId, UUID playerId, int eraNumber, String window, int roundNumber);

    @Query("""
            select distinct s.playerId from PlayerSubmissionEntity s
            where s.gameId = :gameId and s.eraNumber = :eraNumber
            and s.submissionWindow = :window and s.roundNumber = :roundNumber
            """)
    List<UUID> findSubmittedPlayerIds(UUID gameId, int eraNumber, String window, int roundNumber);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
