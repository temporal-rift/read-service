package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** One player's accepted decision; reads must always filter by the authenticated player. */
public record PlayerSubmission(
        UUID gameId,
        UUID playerId,
        int eraNumber,
        Integer roundNumber,
        SubmissionWindow window,
        SubmissionChoice choice,
        SubmittedCard card,
        String specialAction,
        Targets targets) {

    public PlayerSubmission {
        if ((window == SubmissionWindow.ACTION) != (roundNumber != null)) {
            throw new IllegalArgumentException("Round number is required only for an action decision");
        }
        if (choice == SubmissionChoice.PASS && (card != null || specialAction != null || targets != null)) {
            throw new IllegalArgumentException("A pass cannot carry decision detail");
        }
    }

    public PlayerSubmission(
            UUID gameId,
            UUID playerId,
            int eraNumber,
            Integer roundNumber,
            SubmissionWindow window,
            SubmissionChoice choice) {
        this(gameId, playerId, eraNumber, roundNumber, window, choice, null, null, null);
    }

    public enum SubmissionWindow {
        HAND_SELECTION,
        DECLARATION,
        ACTION,
        PARADOX_RESOLUTION
    }

    public enum SubmissionChoice {
        CARD,
        SPECIAL,
        PASS
    }

    public record SubmittedCard(UUID cardInstanceId, String cardType, String grade, String disguiseCategory) {}

    public record Targets(
            UUID targetEventId,
            List<UUID> targetEventIds,
            UUID targetOutcomeId,
            UUID sourceEventId,
            UUID sourceOutcomeId,
            UUID targetPlayerId,
            List<UUID> targetPlayerIds) {
        public Targets {
            targetEventIds = targetEventIds == null ? null : List.copyOf(targetEventIds);
            targetPlayerIds = targetPlayerIds == null ? null : List.copyOf(targetPlayerIds);
        }

        public boolean isEmpty() {
            return targetEventId == null
                    && targetEventIds == null
                    && targetOutcomeId == null
                    && sourceEventId == null
                    && sourceOutcomeId == null
                    && targetPlayerId == null
                    && targetPlayerIds == null;
        }
    }
}
