package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;

/** Stores each player's own accepted decisions, at most one per decision slot. */
public interface PlayerSubmissionRepository {

    List<PlayerSubmission> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber);

    void upsert(PlayerSubmission submission);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
