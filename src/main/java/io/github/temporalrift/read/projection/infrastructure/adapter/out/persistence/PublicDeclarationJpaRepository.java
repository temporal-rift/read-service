package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface PublicDeclarationJpaRepository extends JpaRepository<PublicDeclarationEntity, UUID> {

    List<PublicDeclarationEntity> findByGameIdAndEraNumberOrderByPlayerIdAsc(UUID gameId, int eraNumber);

    Optional<PublicDeclarationEntity> findByGameIdAndEraNumberAndPlayerId(UUID gameId, int eraNumber, UUID playerId);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
