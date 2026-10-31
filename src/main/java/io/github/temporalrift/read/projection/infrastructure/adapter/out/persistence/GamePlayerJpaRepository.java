package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface GamePlayerJpaRepository extends JpaRepository<GamePlayerEntity, UUID> {

    /** Seat order; unseated players last, the player id deciding among them. */
    @Query("""
            select p from GamePlayerEntity p
            where p.gameId = :gameId
            order by p.seatIndex asc nulls last, p.playerId asc""")
    List<GamePlayerEntity> findByGameIdInSeatOrder(@Param("gameId") UUID gameId);

    Optional<GamePlayerEntity> findByGameIdAndPlayerId(UUID gameId, UUID playerId);
}
