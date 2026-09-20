package io.github.temporalrift.read.projection.domain.port.out;

import java.util.UUID;

/**
 * Tracks per game era whether the band correction has superseded the preview, so a reordered or
 * resubmitted preview can never overwrite corrected bands. Markers are era-scoped and cleared
 * with the rest of the recoverable state.
 */
public interface BandCorrectionRepository {

    boolean isCorrectionApplied(UUID gameId, int eraNumber);

    void markCorrectionApplied(UUID gameId, int eraNumber);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
