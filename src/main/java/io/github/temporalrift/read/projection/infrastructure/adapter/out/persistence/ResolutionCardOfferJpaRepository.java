package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface ResolutionCardOfferJpaRepository extends JpaRepository<ResolutionCardOfferEntity, UUID> {
    Optional<ResolutionCardOfferEntity> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
