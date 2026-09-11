package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface RevealedProbabilityIntelJpaRepository extends JpaRepository<RevealedProbabilityIntelEntity, UUID> {

    List<RevealedProbabilityIntelEntity> findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(
            UUID gameId, UUID playerId, int eraNumber);

    Optional<RevealedProbabilityIntelEntity> findByGameIdAndPlayerIdAndEraNumberAndEventId(
            UUID gameId, UUID playerId, int eraNumber, UUID eventId);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
