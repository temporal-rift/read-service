package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface RevealedHandCardIntelJpaRepository extends JpaRepository<RevealedHandCardIntelEntity, UUID> {

    List<RevealedHandCardIntelEntity> findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(
            UUID gameId, UUID playerId, int eraNumber);

    Optional<RevealedHandCardIntelEntity> findByGameIdAndPlayerIdAndEraNumberAndEventId(
            UUID gameId, UUID playerId, int eraNumber, UUID eventId);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);
}
