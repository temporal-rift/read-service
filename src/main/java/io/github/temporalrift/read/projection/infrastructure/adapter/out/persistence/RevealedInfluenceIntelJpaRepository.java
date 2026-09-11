package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface RevealedInfluenceIntelJpaRepository extends JpaRepository<RevealedInfluenceIntelEntity, UUID> {

    List<RevealedInfluenceIntelEntity> findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(
            UUID gameId, UUID playerId, int eraNumber);

    Optional<RevealedInfluenceIntelEntity> findByGameIdAndPlayerIdAndEraNumberAndEventId(
            UUID gameId, UUID playerId, int eraNumber, UUID eventId);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
