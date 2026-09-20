package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface ExposeFactJpaRepository extends JpaRepository<ExposeFactEntity, UUID> {

    List<ExposeFactEntity> findByGameIdAndEraNumberOrderByActivistPlayerIdAscTargetPlayerIdAscRoundNumberAsc(
            UUID gameId, int eraNumber);

    Optional<ExposeFactEntity> findByGameIdAndEraNumberAndActivistPlayerIdAndTargetPlayerIdAndRoundNumber(
            UUID gameId, int eraNumber, UUID activistPlayerId, UUID targetPlayerId, int roundNumber);

    @Transactional
    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    @Transactional
    void deleteByGameId(UUID gameId);
}
