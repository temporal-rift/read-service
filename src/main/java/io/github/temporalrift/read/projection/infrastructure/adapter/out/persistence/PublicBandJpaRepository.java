package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface PublicBandJpaRepository extends JpaRepository<PublicBandEntity, UUID> {

    List<PublicBandEntity> findByGameIdAndEraNumberOrderByEventIdAsc(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
