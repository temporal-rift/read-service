package io.github.temporalrift.read.simulation.application.command;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.simulation.application.port.in.CloseSimulationExecutionUseCase;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContextConflictException;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionNotConfiguredException;
import io.github.temporalrift.read.simulation.domain.port.out.ClockOperationRepository;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@Service
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class CloseSimulationExecutionCommandHandler implements CloseSimulationExecutionUseCase {

    private final ExecutionRepository executions;
    private final ClockOperationRepository operations;
    private final List<GameplayState> gameplay;
    private final SimulationControlLock controlLock;
    private final TransactionTemplate transaction;

    CloseSimulationExecutionCommandHandler(
            ExecutionRepository executions,
            ClockOperationRepository operations,
            List<GameplayState> gameplay,
            SimulationControlLock controlLock,
            TransactionTemplate transaction) {
        this.executions = executions;
        this.operations = operations;
        this.gameplay = gameplay;
        this.controlLock = controlLock;
        this.transaction = transaction;
    }

    /**
     * The context's projections and claims are discarded in the transaction that closes it. A record handler holds the
     * context until it commits, so the close waits for a handler that admitted a record and a later handler finds no
     * context: nothing can write into what the close emptied. Records still in a source are fenced when the next
     * context opens.
     */
    @Override
    public void handle(UUID caseKey) {
        controlLock.lock();
        try {
            transaction.executeWithoutResult(status -> close(caseKey));
        } finally {
            controlLock.unlock();
        }
    }

    private void close(UUID caseKey) {
        var open = executions.findWithLock();
        if (open.isPresent() && open.get().context().caseKey().equals(caseKey)) {
            gameplay.forEach(GameplayState::discardAll);
            operations.clear();
            executions.close(open.get());
            return;
        }
        var lastClosed = executions.lastClosedCaseKey();
        if (open.isEmpty() && lastClosed.isEmpty()) {
            throw new ExecutionNotConfiguredException();
        }
        // Repeating the last closed case is a no-op, whether or not another context opened since.
        if (!caseKey.equals(lastClosed.orElse(null))) {
            throw new ExecutionContextConflictException("The case is neither the open context nor the last closed one");
        }
    }
}
