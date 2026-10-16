package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmissionChoice;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmissionWindow;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmittedCard;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;

public abstract class PlayerSubmissionContract extends PortContract {

    private final UUID playerId = UUID.randomUUID();

    protected abstract PlayerSubmissionRepository port();

    @Test
    void submissionsOfAPlayerAreOrderedByWindowThenRound() {
        var action2 = pass(playerId, 1, SubmissionWindow.ACTION, 2);
        var action1 = pass(playerId, 1, SubmissionWindow.ACTION, 1);
        var declaration = pass(playerId, 1, SubmissionWindow.DECLARATION, null);
        var paradox = pass(playerId, 1, SubmissionWindow.PARADOX_RESOLUTION, null);
        inTransaction(() -> {
            port().upsert(paradox);
            port().upsert(action2);
            port().upsert(declaration);
            port().upsert(action1);
        });

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .containsExactly(action1, action2, declaration, paradox);
    }

    @Test
    void aSubmissionKeepsItsCardDetail() {
        var submission = new PlayerSubmission(
                gameId,
                playerId,
                1,
                1,
                SubmissionWindow.ACTION,
                SubmissionChoice.CARD,
                new SubmittedCard(UUID.randomUUID(), "PUSH", "II", null),
                null,
                new PlayerSubmission.Targets(UUID.randomUUID(), null, UUID.randomUUID(), null, null, null, null));

        inTransaction(() -> port().upsert(submission));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .containsExactly(submission);
    }

    @Test
    void upsertReplacesTheDecisionOfTheSameSlot() {
        inTransaction(() -> port().upsert(pass(playerId, 1, SubmissionWindow.ACTION, 1)));

        var card = new PlayerSubmission(
                gameId, playerId, 1, 1, SubmissionWindow.ACTION, SubmissionChoice.SPECIAL, null, "EXPOSE", null);
        inTransaction(() -> port().upsert(card));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .containsExactly(card);
    }

    @Test
    void submittedPlayersAreThoseWithADecisionInTheSlot() {
        var other = UUID.randomUUID();
        inTransaction(() -> {
            port().upsert(pass(playerId, 1, SubmissionWindow.ACTION, 1));
            port().upsert(pass(other, 1, SubmissionWindow.ACTION, 1));
            port().upsert(pass(UUID.randomUUID(), 1, SubmissionWindow.ACTION, 2));
            port().upsert(pass(UUID.randomUUID(), 2, SubmissionWindow.ACTION, 1));
            port().upsert(pass(UUID.randomUUID(), 1, SubmissionWindow.DECLARATION, null));
        });

        assertThat(inTransaction(() -> port().findSubmittedPlayerIds(gameId, 1, SubmissionWindow.ACTION, 1)))
                .containsExactlyInAnyOrder(playerId, other);
        assertThat(inTransaction(() -> port().findSubmittedPlayerIds(gameId, 1, SubmissionWindow.DECLARATION, null)))
                .hasSize(1);
        assertThat(inTransaction(() -> port().findSubmittedPlayerIds(otherGameId, 1, SubmissionWindow.ACTION, 1)))
                .isEmpty();
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = pass(playerId, 1, SubmissionWindow.DECLARATION, null);
        var era2 = pass(playerId, 2, SubmissionWindow.DECLARATION, null);
        inTransaction(() -> {
            port().upsert(era1);
            port().upsert(era2);
        });

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .containsExactly(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .isEmpty();
    }

    private PlayerSubmission pass(UUID player, int eraNumber, SubmissionWindow window, Integer roundNumber) {
        return new PlayerSubmission(gameId, player, eraNumber, roundNumber, window, SubmissionChoice.PASS);
    }
}
