package io.github.temporalrift.read.simulation.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExecutionTest {

    private static final Instant NOW = Instant.parse("2026-10-05T19:00:00.123456Z");
    private static final String DIGEST = "a".repeat(64);
    private static final UUID GAME_ID = UUID.randomUUID();

    private final ExecutionContext context = context();

    @Test
    void configureStartsAtRevisionZeroWithoutAGame() {
        var execution = Execution.configure(context);

        assertThat(execution.context()).isEqualTo(context);
        assertThat(execution.revision()).isZero();
        assertThat(execution.logicalTime()).isEqualTo(context.logicalTime());
        assertThat(execution.gameId()).isNull();
    }

    @Test
    void aRecordedOperationWithTheSameRequestIsReplayedWithoutApplyingItAgain() {
        var request = advance(0, NOW.plusSeconds(60));
        var recorded = operation(request);
        var execution = new Execution(context, 3, NOW.plusSeconds(30), GAME_ID);

        var result = execution.advance(request, recorded);

        assertThat(result.applied()).isFalse();
        assertThat(result.execution()).isSameAs(execution);
        assertThat(result.operation()).isSameAs(recorded);
    }

    @Test
    void aRecordedOperationWithADifferentRequestIsAConflict() {
        var request = advance(0, NOW.plusSeconds(60));
        var recorded = operation(advance(1, NOW.plusSeconds(60)));
        var execution = Execution.configure(context);

        assertThatThrownBy(() -> execution.advance(request, recorded))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining(request.operationId().toString());
    }

    @Test
    void anExpectedRevisionThatIsNotCurrentIsRejected() {
        var execution = new Execution(context, 2, NOW, null);
        var request = advance(1, NOW.plusSeconds(10));

        assertThatThrownBy(() -> execution.advance(request, null))
                .isInstanceOf(StaleExecutionRevisionException.class)
                .hasMessageContaining("Expected revision 1");
    }

    @Test
    void aTargetBeforeTheLogicalTimeIsRejected() {
        var execution = new Execution(context, 0, NOW, null);
        var request = advance(0, NOW.minusSeconds(1));

        assertThatThrownBy(() -> execution.advance(request, null)).isInstanceOf(ClockRegressionException.class);
    }

    @Test
    void anAdvanceMovesTheRevisionAndKeepsTheBoundGame() {
        var execution = new Execution(context, 4, NOW, GAME_ID);
        var request = advance(4, NOW.plusSeconds(90));

        var result = execution.advance(request, null);

        assertThat(result.applied()).isTrue();
        assertThat(result.execution().revision()).isEqualTo(5);
        assertThat(result.execution().logicalTime()).isEqualTo(request.targetTime());
        assertThat(result.execution().gameId()).isEqualTo(GAME_ID);
        assertThat(result.operation().request()).isEqualTo(request);
        assertThat(result.operation().acknowledgement())
                .isEqualTo(new ClockAcknowledgement(request.operationId(), 5, request.targetTime()));
    }

    @Test
    void stateIsReadyUntilAGameIsBound() {
        var execution = Execution.configure(context);

        assertThat(execution.state(false)).isEqualTo(ExecutionState.READY);
        assertThat(execution.state(true)).isEqualTo(ExecutionState.READY);
    }

    @Test
    void stateIsActiveUntilTheBoundGameEnds() {
        var execution = new Execution(context, 0, NOW, GAME_ID);

        assertThat(execution.state(false)).isEqualTo(ExecutionState.ACTIVE);
        assertThat(execution.state(true)).isEqualTo(ExecutionState.TERMINAL);
    }

    private static ClockAdvance advance(long expectedRevision, Instant targetTime) {
        return new ClockAdvance(UUID.randomUUID(), expectedRevision, targetTime);
    }

    private static ClockOperation operation(ClockAdvance request) {
        return new ClockOperation(request, new ClockAcknowledgement(request.operationId(), 7, request.targetTime()));
    }

    private static ExecutionContext context() {
        var rules = new EffectiveRules(Map.of("game.rules.win-score-threshold", 20));
        return ExecutionContext.of(
                UUID.randomUUID(),
                new Seed("42"),
                EntropyVersion.SHA256_V1,
                DIGEST,
                rules,
                rules.digest(),
                NOW,
                List.of(
                        new SimulationSeat(0, UUID.randomUUID(), "ERASERS"),
                        new SimulationSeat(1, UUID.randomUUID(), "WEAVERS"),
                        new SimulationSeat(2, UUID.randomUUID(), "PROPHETS")));
    }
}
