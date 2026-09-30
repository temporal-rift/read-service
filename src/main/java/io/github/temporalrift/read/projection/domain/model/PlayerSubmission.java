package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** One player's own accepted decision for recovery after reload — never another player's. */
public record PlayerSubmission(
        UUID gameId,
        UUID playerId,
        int eraNumber,
        Integer roundNumber,
        SubmissionWindow window,
        SubmissionChoice choice) {

    /** Round is present only for action-round submissions. */
    public enum SubmissionWindow {
        HAND_SELECTION,
        DECLARATION,
        ACTION,
        PARADOX_RESOLUTION
    }

    /** Present only for action-round and paradox-resolution windows. */
    public enum SubmissionChoice {
        CARD,
        SPECIAL,
        PASS
    }
}
