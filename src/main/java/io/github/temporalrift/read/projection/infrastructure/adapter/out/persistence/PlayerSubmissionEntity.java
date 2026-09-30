package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;

/**
 * One player's accepted decision slot. {@code round_number} is the action round for ordinary
 * submissions and {@code 0} for hand selection, declarations and paradox resolution, which carry no round.
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "card", columnDefinition = "jsonb")
    private String card;

    @Column(name = "special_action")
    private String specialAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "targets", columnDefinition = "jsonb")
    private String targets;

    protected PlayerSubmissionEntity() {}

    private PlayerSubmissionEntity(UUID id, PlayerSubmission submission, ObjectMapper objectMapper) {
        super(id, submission.gameId(), submission.playerId(), submission.eraNumber());
        updateFrom(submission, objectMapper);
    }

    static PlayerSubmissionEntity fromDomain(UUID id, PlayerSubmission submission, ObjectMapper objectMapper) {
        return new PlayerSubmissionEntity(id, submission, objectMapper);
    }

    PlayerSubmission toDomain(ObjectMapper objectMapper) {
        return new PlayerSubmission(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                roundNumber == 0 ? null : roundNumber,
                PlayerSubmission.SubmissionWindow.valueOf(submissionWindow),
                choice == null ? null : PlayerSubmission.SubmissionChoice.valueOf(choice),
                card == null ? null : objectMapper.readValue(card, PlayerSubmission.SubmittedCard.class),
                specialAction,
                targets == null ? null : objectMapper.readValue(targets, PlayerSubmission.Targets.class));
    }

    void updateFrom(PlayerSubmission submission, ObjectMapper objectMapper) {
        this.roundNumber = submission.roundNumber() == null ? 0 : submission.roundNumber();
        this.submissionWindow = submission.window().name();
        this.choice = submission.choice() == null ? null : submission.choice().name();
        this.card = submission.card() == null ? null : objectMapper.writeValueAsString(submission.card());
        this.specialAction = submission.specialAction();
        this.targets = submission.targets() == null ? null : objectMapper.writeValueAsString(submission.targets());
    }
}
