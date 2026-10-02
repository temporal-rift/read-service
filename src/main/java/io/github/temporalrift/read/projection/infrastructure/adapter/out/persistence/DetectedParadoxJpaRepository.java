package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface DetectedParadoxJpaRepository extends JpaRepository<DetectedParadoxEntity, UUID> {
    List<DetectedParadoxEntity> findByGameIdAndParadoxIdIn(UUID gameId, Collection<UUID> paradoxIds);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
