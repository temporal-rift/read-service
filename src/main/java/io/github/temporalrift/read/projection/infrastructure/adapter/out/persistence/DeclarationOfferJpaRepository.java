package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface DeclarationOfferJpaRepository extends JpaRepository<DeclarationOfferEntity, UUID> {
    Optional<DeclarationOfferEntity> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
