package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** One player's own accepted decision for recovery after reload — never another player's. */
public record PlayerSubmission(
        UUID gameId, UUID playerId, int eraNumber, Integer roundNumber, SubmissionKind kind, String actionType) {

    /** Round is present only for ordinary action-round submissions. */
    public enum SubmissionKind {
        HAND_SELECTION,
        DECLARATION,
        ACTION,
        PARADOX_CARD
    }
}
