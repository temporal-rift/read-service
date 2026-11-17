package io.github.temporalrift.read.simulation.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;
import io.github.temporalrift.read.simulation.domain.execution.ClockAcknowledgement;
import io.github.temporalrift.read.simulation.domain.execution.ClockAdvance;
import io.github.temporalrift.read.simulation.domain.execution.ClockOperation;
import io.github.temporalrift.read.simulation.domain.execution.EffectiveRules;
import io.github.temporalrift.read.simulation.domain.execution.EntropyVersion;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.InvalidClockAdvanceException;
import io.github.temporalrift.read.simulation.domain.execution.Seed;
import io.github.temporalrift.read.simulation.domain.execution.SimulationSeat;
import io.github.temporalrift.read.simulation.domain.execution.StaleExecutionRevisionException;
import io.github.temporalrift.read.simulation.domain.port.out.ClockOperationRepository;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@ExtendWith(MockitoExtension.class)
class AdvanceSimulationClockCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T19:00:00.123456Z");
    private static final String DIGEST = "a".repeat(64);

    @Mock
    ExecutionRepository executions;

    @Mock
    ClockOperationRepository operations;

    @Test
    void anAdvanceWithoutAnOpenContextIsNotConfigured() {
        given(executions.findWithLock()).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler().handle(new ClockAdvance(UUID.randomUUID(), 0, NOW)))
                .hasMessage("No simulation execution has been configured");

        verify(operations, never()).find(any());
    }

    @Test
    void anAppliedAdvanceStoresTheNewExecutionAndItsOperation() {
        var execution = Execution.configure(context());
        given(executions.findWithLock()).willReturn(Optional.of(execution));
        var request = new ClockAdvance(UUID.randomUUID(), 0, NOW.plusSeconds(60));
        given(operations.find(request.operationId())).willReturn(Optional.empty());

        var acknowledgement = handler().handle(request);

        var stored = ArgumentCaptor.forClass(Execution.class);
        verify(executions).update(stored.capture());
        assertThat(stored.getValue().revision()).isEqualTo(1);
        assertThat(stored.getValue().logicalTime()).isEqualTo(request.targetTime());
        verify(operations).save(any());
        assertThat(acknowledgement).isEqualTo(new ClockAcknowledgement(request.operationId(), 1, request.targetTime()));
    }

    @Test
    void aRepeatedAdvanceReturnsTheRecordedAcknowledgementWithoutWriting() {
        var execution = Execution.configure(context());
        var request = new ClockAdvance(UUID.randomUUID(), 0, NOW.plusSeconds(60));
        var recorded =
                new ClockOperation(request, new ClockAcknowledgement(request.operationId(), 9, request.targetTime()));
        given(executions.findWithLock()).willReturn(Optional.of(execution));
        given(operations.find(request.operationId())).willReturn(Optional.of(recorded));

        assertThat(handler().handle(request)).isEqualTo(recorded.acknowledgement());

        verify(executions, never()).update(any());
        verify(operations, never()).save(any());
    }

    @Test
    void aRepeatWithADifferentBodyIsRejectedWithoutWriting() {
        var execution = Execution.configure(context());
        var request = new ClockAdvance(UUID.randomUUID(), 0, NOW.plusSeconds(60));
        var recorded = new ClockOperation(
                new ClockAdvance(request.operationId(), 0, NOW.plusSeconds(30)),
                new ClockAcknowledgement(request.operationId(), 1, NOW.plusSeconds(30)));
        given(executions.findWithLock()).willReturn(Optional.of(execution));
        given(operations.find(request.operationId())).willReturn(Optional.of(recorded));

        assertThatThrownBy(() -> handler().handle(request))
                .hasMessageContaining(request.operationId().toString());

        verify(executions, never()).update(any());
        verify(operations, never()).save(any());
    }

    @Test
    void aStaleRevisionIsRejectedWithoutWriting() {
        var execution = Execution.configure(context());
        given(executions.findWithLock()).willReturn(Optional.of(execution));

        assertThatThrownBy(() -> handler().handle(new ClockAdvance(UUID.randomUUID(), 1, NOW)))
                .isInstanceOf(StaleExecutionRevisionException.class);

        verify(executions, never()).update(any());
        verify(operations, never()).save(any());
    }

    @Test
    void anInvalidAdvanceIsRejectedBeforeTouchingTheContext() {
        assertThatThrownBy(() -> new ClockAdvance(UUID.randomUUID(), -1, NOW))
                .isInstanceOf(InvalidClockAdvanceException.class);
    }

    private AdvanceSimulationClockCommandHandler handler() {
        return new AdvanceSimulationClockCommandHandler(
                executions,
                operations,
                new TransactionTemplate(new InMemoryTransactionManager(new InMemoryDatabase())));
    }

    static ExecutionContext context() {
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
