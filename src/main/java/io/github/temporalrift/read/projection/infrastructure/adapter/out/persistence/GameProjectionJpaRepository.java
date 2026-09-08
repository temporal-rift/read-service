package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GameProjectionJpaRepository extends JpaRepository<GameProjectionEntity, UUID> {

    @Modifying
    @Query(value = """
                    INSERT INTO game_projection (game_id, era_number, phase)
                    VALUES (:gameId, 0, 'LOBBY')
                    ON CONFLICT (game_id) DO NOTHING
                    """, nativeQuery = true)
    int insertAnchorIfAbsent(@Param("gameId") UUID gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameProjectionEntity g where g.gameId = :gameId")
    Optional<GameProjectionEntity> lockByGameId(@Param("gameId") UUID gameId);
}
