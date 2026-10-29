package io.github.temporalrift.read.simulation.application.command;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.simulation.application.port.in.CloseSimulationExecutionUseCase;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContextConflictException;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionNotConfiguredException;
import io.github.temporalrift.read.simulation.domain.port.out.ClockOperationRepository;
import io.github.temporalrift.read.simulation.domain.port.out.EventFence;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@Service
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class CloseSimulationExecutionCommandHandler implements CloseSimulationExecutionUseCase {

    private final ExecutionRepository executions;
    private final ClockOperationRepository operations;
    private final List<GameplayState> gameplay;
    private final EventFence eventFence;
    private final SimulationControlLock controlLock;
    private final TransactionTemplate transaction;

    CloseSimulationExecutionCommandHandler(
            ExecutionRepository executions,
            ClockOperationRepository operations,
            List<GameplayState> gameplay,
            EventFence eventFence,
            SimulationControlLock controlLock,
            TransactionTemplate transaction) {
        this.executions = executions;
        this.operations = operations;
        this.gameplay = gameplay;
        this.eventFence = eventFence;
        this.controlLock = controlLock;
        this.transaction = transaction;
    }

    /**
     * The context's projections and claims are discarded in the transaction that closes it. A record handler holds the
     * context until it commits, so the close waits for a handler that admitted a record and a later handler finds no
     * context: nothing can write into what the close emptied.
     */
    @Override
    public void handle(UUID caseKey) {
        controlLock.lock();
        try {
            var outcome = transaction.execute(status -> close(caseKey));
            if (outcome != Outcome.LAST_CLOSED_WHILE_OPEN) {
                // Recorded before the lock is released: the next context cannot open while the fence is captured. A
                // repeated close captures it again, completing a capture that failed after its close committed.
                eventFence.capture();
            }
        } finally {
            controlLock.unlock();
        }
    }

    private Outcome close(UUID caseKey) {
        var open = executions.findWithLock();
        if (open.isPresent()) {
            return closeOpen(open.get(), caseKey);
        }
        var lastClosed = executions.lastClosedCaseKey().orElseThrow(ExecutionNotConfiguredException::new);
        requireClosedEarlier(lastClosed, caseKey);
        return Outcome.LAST_CLOSED;
    }

    private Outcome closeOpen(Execution open, UUID caseKey) {
        if (open.context().caseKey().equals(caseKey)) {
            gameplay.forEach(GameplayState::discardAll);
            operations.clear();
            executions.close(open);
            return Outcome.CLOSED;
        }
        requireClosedEarlier(executions.lastClosedCaseKey().orElse(null), caseKey);
        // Repeating the last closed case while another is open is a no-op; it must not fence the open context.
        return Outcome.LAST_CLOSED_WHILE_OPEN;
    }

    private static void requireClosedEarlier(UUID lastClosed, UUID caseKey) {
        if (!caseKey.equals(lastClosed)) {
            throw new ExecutionContextConflictException("The case is neither the open context nor the last closed one");
        }
    }

    private enum Outcome {
        CLOSED,
        LAST_CLOSED,
        LAST_CLOSED_WHILE_OPEN
    }
}
