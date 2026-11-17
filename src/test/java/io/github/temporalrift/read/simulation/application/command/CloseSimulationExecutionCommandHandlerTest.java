package io.github.temporalrift.read.simulation.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;
import io.github.temporalrift.read.simulation.domain.execution.EffectiveRules;
import io.github.temporalrift.read.simulation.domain.execution.EntropyVersion;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContextConflictException;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionNotConfiguredException;
import io.github.temporalrift.read.simulation.domain.execution.Seed;
import io.github.temporalrift.read.simulation.domain.execution.SimulationSeat;
import io.github.temporalrift.read.simulation.domain.port.out.ClockOperationRepository;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@ExtendWith(MockitoExtension.class)
class CloseSimulationExecutionCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T19:00:00Z");
    private static final String DIGEST = "a".repeat(64);

    @Mock
    ExecutionRepository executions;

    @Mock
    ClockOperationRepository operations;

    @Mock
    GameplayState projections;

    @Mock
    GameplayState claims;

    @Mock
    SimulationControlLock controlLock;

    @Test
    void closingTheOpenContextDiscardsItsStateAndClosesIt() {
        var open = Execution.configure(context());
        given(executions.findWithLock()).willReturn(Optional.of(open));

        handler().handle(open.context().caseKey());

        verify(projections).discardAll();
        verify(claims).discardAll();
        verify(operations).clear();
        verify(executions).close(open);
        var order = inOrder(controlLock);
        order.verify(controlLock).lock();
        order.verify(controlLock).unlock();
    }

    @Test
    void aDifferentCaseWhileAnotherContextIsOpenIsAConflict() {
        var open = Execution.configure(context());
        given(executions.findWithLock()).willReturn(Optional.of(open));
        given(executions.lastClosedCaseKey()).willReturn(Optional.empty());
        var handler = handler();
        var caseKey = UUID.randomUUID();

        assertThatThrownBy(() -> handler.handle(caseKey)).isInstanceOf(ExecutionContextConflictException.class);

        verify(projections, never()).discardAll();
        verify(operations, never()).clear();
        verify(executions, never()).close(open);
        verify(controlLock).unlock();
    }

    @Test
    void closingWithNothingOpenOrClosedIsNotConfigured() {
        given(executions.findWithLock()).willReturn(Optional.empty());
        given(executions.lastClosedCaseKey()).willReturn(Optional.empty());
        var handler = handler();
        var caseKey = UUID.randomUUID();

        assertThatThrownBy(() -> handler.handle(caseKey)).isInstanceOf(ExecutionNotConfiguredException.class);

        verify(controlLock).unlock();
    }

    @Test
    void repeatingTheLastClosedCaseIsANoOp() {
        var lastClosed = UUID.randomUUID();
        given(executions.findWithLock()).willReturn(Optional.empty());
        given(executions.lastClosedCaseKey()).willReturn(Optional.of(lastClosed));

        handler().handle(lastClosed);

        verify(projections, never()).discardAll();
        verify(operations, never()).clear();
        verify(executions, never()).close(any());
    }

    @Test
    void repeatingTheLastClosedCaseWhileANewContextIsOpenIsANoOp() {
        var lastClosed = UUID.randomUUID();
        var open = Execution.configure(context());
        given(executions.findWithLock()).willReturn(Optional.of(open));
        given(executions.lastClosedCaseKey()).willReturn(Optional.of(lastClosed));

        handler().handle(lastClosed);

        verify(projections, never()).discardAll();
        verify(operations, never()).clear();
        verify(executions, never()).close(open);
    }

    @Test
    void aCaseThatIsNeitherOpenNorLastClosedIsAConflict() {
        var lastClosed = UUID.randomUUID();
        given(executions.findWithLock()).willReturn(Optional.empty());
        given(executions.lastClosedCaseKey()).willReturn(Optional.of(lastClosed));
        var handler = handler();
        var caseKey = UUID.randomUUID();

        assertThatThrownBy(() -> handler.handle(caseKey)).isInstanceOf(ExecutionContextConflictException.class);

        verify(projections, never()).discardAll();
    }

    private CloseSimulationExecutionCommandHandler handler() {
        return new CloseSimulationExecutionCommandHandler(
                executions,
                operations,
                List.of(projections, claims),
                controlLock,
                new TransactionTemplate(new InMemoryTransactionManager(new InMemoryDatabase())));
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
