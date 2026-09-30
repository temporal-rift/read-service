package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;

/**
 * One player's accepted decision slot. {@code round_number} is the action round for ordinary
 * submissions and {@code 0} for every other window, which carries no round.
 */
@Entity
@Table(
        name = "player_submission",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_player_submission_identity",
                        columnNames = {"game_id", "player_id", "era_number", "submission_window", "round_number"}))
class PlayerSubmissionEntity extends PlayerScopedBaseEntity {

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "submission_window", nullable = false)
    private String submissionWindow;

    @Column(name = "choice")
    private String choice;

    protected PlayerSubmissionEntity() {}

    private PlayerSubmissionEntity(UUID id, PlayerSubmission submission) {
        super(id, submission.gameId(), submission.playerId(), submission.eraNumber());
        updateFrom(submission);
    }

    static PlayerSubmissionEntity fromDomain(UUID id, PlayerSubmission submission) {
        return new PlayerSubmissionEntity(id, submission);
    }

    PlayerSubmission toDomain() {
        return new PlayerSubmission(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                roundNumber == 0 ? null : roundNumber,
                PlayerSubmission.SubmissionWindow.valueOf(submissionWindow),
                choice == null ? null : PlayerSubmission.SubmissionChoice.valueOf(choice));
    }

    void updateFrom(PlayerSubmission submission) {
        this.roundNumber = submission.roundNumber() == null ? 0 : submission.roundNumber();
        this.submissionWindow = submission.window().name();
        this.choice = submission.choice() == null ? null : submission.choice().name();
    }
}
