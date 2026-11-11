package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.SpecialAvailabilityReport;

/** Stores each player's latest special-action availability report, one per game and player. */
public interface SpecialAvailabilityRepository {

    Optional<SpecialAvailabilityReport> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    /** Inserts or replaces the player's report; the caller decides whether it is the latest. */
    void save(SpecialAvailabilityReport report);

    void deleteByGameId(UUID gameId);
}
