package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;

/**
 * One player's accepted decision slot. {@code round_number} is the action round for ordinary
 * submissions and {@code 0} for hand selection, declarations and paradox cards, which carry no round.
 */
@Entity
@Table(
        name = "player_submission",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_player_submission_identity",
                        columnNames = {"game_id", "player_id", "era_number", "kind", "round_number"}))
class PlayerSubmissionEntity extends PlayerScopedBaseEntity {

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "action_type")
    private String actionType;

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
                PlayerSubmission.SubmissionKind.valueOf(kind),
                actionType);
    }

    void updateFrom(PlayerSubmission submission) {
        this.roundNumber = submission.roundNumber() == null ? 0 : submission.roundNumber();
        this.kind = submission.kind().name();
        this.actionType = submission.actionType();
    }
}
