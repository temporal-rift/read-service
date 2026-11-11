package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface SpecialAvailabilityJpaRepository extends JpaRepository<SpecialAvailabilityEntity, UUID> {

    Optional<SpecialAvailabilityEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    @Transactional
    void deleteByGameId(UUID gameId);
}
