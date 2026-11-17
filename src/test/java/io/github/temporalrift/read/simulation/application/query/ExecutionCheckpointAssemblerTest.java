package io.github.temporalrift.read.simulation.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.shared.GameEndProbe;
import io.github.temporalrift.read.simulation.domain.execution.EffectiveRules;
import io.github.temporalrift.read.simulation.domain.execution.EntropyVersion;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionState;
import io.github.temporalrift.read.simulation.domain.execution.Seed;
import io.github.temporalrift.read.simulation.domain.execution.SimulationSeat;
import io.github.temporalrift.read.simulation.domain.execution.SourceWatermark;
import io.github.temporalrift.read.simulation.domain.port.out.SourceWatermarks;

@ExtendWith(MockitoExtension.class)
class ExecutionCheckpointAssemblerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T19:00:00.123456Z");
    private static final String DIGEST = "a".repeat(64);
    private static final List<SourceWatermark> WATERMARKS =
            List.of(new SourceWatermark("group", "game.events", 3, 42L));

    @Mock
    GameEndProbe gameEnd;

    @Mock
    SourceWatermarks sourceWatermarks;

    @Test
    void aContextWithoutAGameIsReadyAndDoesNotProbeTheGame() {
        var execution = Execution.configure(context());
        given(sourceWatermarks.current()).willReturn(WATERMARKS);

        var checkpoint = assembler().assemble(execution);

        assertThat(checkpoint.state()).isEqualTo(ExecutionState.READY);
        assertThat(checkpoint.gameId()).isNull();
        verifyNoInteractions(gameEnd);
    }

    @Test
    void aBoundGameIsActiveUntilItsEndIsApplied() {
        var gameId = UUID.randomUUID();
        var execution = new Execution(context(), 3, NOW, gameId);
        given(gameEnd.hasEnded(gameId)).willReturn(false);
        given(sourceWatermarks.current()).willReturn(WATERMARKS);

        assertThat(assembler().assemble(execution).state()).isEqualTo(ExecutionState.ACTIVE);
    }

    @Test
    void aBoundGameWhoseEndIsAppliedIsTerminal() {
        var gameId = UUID.randomUUID();
        var execution = new Execution(context(), 3, NOW, gameId);
        given(gameEnd.hasEnded(gameId)).willReturn(true);
        given(sourceWatermarks.current()).willReturn(WATERMARKS);

        assertThat(assembler().assemble(execution).state()).isEqualTo(ExecutionState.TERMINAL);
    }

    @Test
    void theCheckpointCarriesTheExecutionIdentityAndWatermarks() {
        var gameId = UUID.randomUUID();
        var execution = new Execution(context(), 7, NOW.plusSeconds(30), gameId);
        given(gameEnd.hasEnded(gameId)).willReturn(false);
        given(sourceWatermarks.current()).willReturn(WATERMARKS);

        var checkpoint = assembler().assemble(execution);

        assertThat(checkpoint.caseKey()).isEqualTo(execution.context().caseKey());
        assertThat(checkpoint.manifestDigest()).isEqualTo(execution.context().manifestDigest());
        assertThat(checkpoint.rulesDigest()).isEqualTo(execution.context().rulesDigest());
        assertThat(checkpoint.revision()).isEqualTo(7);
        assertThat(checkpoint.logicalTime()).isEqualTo(execution.logicalTime());
        assertThat(checkpoint.gameId()).isEqualTo(gameId);
        assertThat(checkpoint.drained()).isTrue();
        assertThat(checkpoint.outboxPending()).isZero();
        assertThat(checkpoint.continuationsPending()).isZero();
        assertThat(checkpoint.dueTimersPending()).isZero();
        assertThat(checkpoint.nextDeadline()).isNull();
        assertThat(checkpoint.sourceWatermarks()).isEqualTo(WATERMARKS);
    }

    private ExecutionCheckpointAssembler assembler() {
        return new ExecutionCheckpointAssembler(gameEnd, sourceWatermarks);
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
