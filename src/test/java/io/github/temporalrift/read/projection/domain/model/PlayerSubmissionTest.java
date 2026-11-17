package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmissionChoice;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmissionWindow;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.Targets;

class PlayerSubmissionTest {

    private static final UUID GAME_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Test
    void anActionDecisionRequiresItsRoundNumber() {
        assertThatThrownBy(() -> new PlayerSubmission(
                        GAME_ID, PLAYER_ID, 1, null, SubmissionWindow.ACTION, SubmissionChoice.CARD))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(new PlayerSubmission(GAME_ID, PLAYER_ID, 1, 2, SubmissionWindow.ACTION, SubmissionChoice.CARD)
                        .roundNumber())
                .isEqualTo(2);
    }

    @Test
    void aNonActionDecisionMustNotCarryARoundNumber() {
        assertThatThrownBy(() -> new PlayerSubmission(
                        GAME_ID, PLAYER_ID, 1, 2, SubmissionWindow.HAND_SELECTION, SubmissionChoice.CARD))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(new PlayerSubmission(
                                GAME_ID, PLAYER_ID, 1, null, SubmissionWindow.HAND_SELECTION, SubmissionChoice.CARD)
                        .roundNumber())
                .isNull();
    }

    @Test
    void aPassCannotCarryDecisionDetail() {
        var card = new PlayerSubmission.SubmittedCard(UUID.randomUUID(), "REWRITE", "II", null);
        var targets = new Targets(UUID.randomUUID(), null, null, null, null, null, null);

        assertThatThrownBy(() -> submission(SubmissionChoice.PASS, card, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> submission(SubmissionChoice.PASS, null, "SCAN", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> submission(SubmissionChoice.PASS, null, null, targets))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPassWithoutDecisionDetailIsAccepted() {
        assertThat(submission(SubmissionChoice.PASS, null, null, null).choice()).isEqualTo(SubmissionChoice.PASS);
    }

    @Test
    void aDecisionMayCarryTargets() {
        var targets = new Targets(UUID.randomUUID(), null, null, null, null, null, null);

        assertThat(submission(SubmissionChoice.SPECIAL, null, "SCAN", targets).targets())
                .isEqualTo(targets);
    }

    @Test
    void nullTargetListsStayNull() {
        var targets = new Targets(null, null, null, null, null, null, null);

        assertThat(targets.targetEventIds()).isNull();
        assertThat(targets.targetPlayerIds()).isNull();
    }

    @Test
    void targetListsAreCopiedSoTheDecisionCannotSeeLaterMutation() {
        var events = new ArrayList<>(List.of(UUID.randomUUID()));
        var players = new ArrayList<>(List.of(UUID.randomUUID()));

        var targets = new Targets(null, events, null, null, null, null, players);
        events.add(UUID.randomUUID());
        players.add(UUID.randomUUID());

        assertThat(targets.targetEventIds()).hasSize(1);
        assertThat(targets.targetPlayerIds()).hasSize(1);
    }

    @Test
    void anEmptyTargetsCarriesNoTargetAtAll() {
        assertThat(new Targets(null, null, null, null, null, null, null).isEmpty())
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void anySingleTargetMakesTargetsNonEmpty(int targetIndex) {
        var targets = targetsWithOnly(targetIndex);

        assertThat(targets.isEmpty()).isFalse();
    }

    private static PlayerSubmission submission(
            SubmissionChoice choice, PlayerSubmission.SubmittedCard card, String specialAction, Targets targets) {
        return new PlayerSubmission(
                GAME_ID, PLAYER_ID, 1, 2, SubmissionWindow.ACTION, choice, card, specialAction, targets);
    }

    private static Targets targetsWithOnly(int targetIndex) {
        var eventId = targetIndex == 0 ? UUID.randomUUID() : null;
        var eventIds = targetIndex == 1 ? List.of(UUID.randomUUID()) : null;
        var outcomeId = targetIndex == 2 ? UUID.randomUUID() : null;
        var sourceEventId = targetIndex == 3 ? UUID.randomUUID() : null;
        var sourceOutcomeId = targetIndex == 4 ? UUID.randomUUID() : null;
        var playerId = targetIndex == 5 ? UUID.randomUUID() : null;
        var playerIds = targetIndex == 6 ? List.of(UUID.randomUUID()) : null;
        return new Targets(eventId, eventIds, outcomeId, sourceEventId, sourceOutcomeId, playerId, playerIds);
    }
}
