package io.github.temporalrift.read.simulation.application.command;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.simulation.application.port.in.AdvanceSimulationClockUseCase;
import io.github.temporalrift.read.simulation.domain.execution.ClockAcknowledgement;
import io.github.temporalrift.read.simulation.domain.execution.ClockAdvance;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionNotConfiguredException;
import io.github.temporalrift.read.simulation.domain.port.out.ClockOperationRepository;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

/** read-service owns no timers: an advance only moves the execution's logical time and revision. */
@Service
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class AdvanceSimulationClockCommandHandler implements AdvanceSimulationClockUseCase {

    private final ExecutionRepository executions;
    private final ClockOperationRepository operations;
    private final TransactionTemplate transaction;

    AdvanceSimulationClockCommandHandler(
            ExecutionRepository executions, ClockOperationRepository operations, TransactionTemplate transaction) {
        this.executions = executions;
        this.operations = operations;
        this.transaction = transaction;
    }

    @Override
    public ClockAcknowledgement handle(ClockAdvance advance) {
        return Objects.requireNonNull(transaction.execute(status -> applyAdvance(advance)));
    }

    private ClockAcknowledgement applyAdvance(ClockAdvance advance) {
        var execution = executions.findWithLock().orElseThrow(ExecutionNotConfiguredException::new);
        var recorded = operations.find(advance.operationId()).orElse(null);
        var outcome = execution.advance(advance, recorded);
        if (outcome.applied()) {
            executions.update(outcome.execution());
            operations.save(outcome.operation());
        }
        return outcome.operation().acknowledgement();
    }
}
