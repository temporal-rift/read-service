package io.github.temporalrift.read.simulation.application.command;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.simulation.application.port.in.ConfigureSimulationExecutionUseCase;
import io.github.temporalrift.read.simulation.application.query.ExecutionCheckpointAssembler;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionCheckpoint;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContextConflictException;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionStateNotCleanException;
import io.github.temporalrift.read.simulation.domain.port.out.EventFence;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

/** read-service uses no rules, so a context is only checked for its shape and for the state it would open over. */
@Service
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class ConfigureSimulationExecutionCommandHandler implements ConfigureSimulationExecutionUseCase {

    private final ExecutionRepository executions;
    private final List<GameplayState> gameplay;
    private final EventFence eventFence;
    private final ExecutionCheckpointAssembler assembler;
    private final SimulationControlLock controlLock;
    private final TransactionTemplate transaction;

    ConfigureSimulationExecutionCommandHandler(
            ExecutionRepository executions,
            List<GameplayState> gameplay,
            EventFence eventFence,
            ExecutionCheckpointAssembler assembler,
            SimulationControlLock controlLock,
            TransactionTemplate transaction) {
        this.executions = executions;
        this.gameplay = gameplay;
        this.eventFence = eventFence;
        this.assembler = assembler;
        this.controlLock = controlLock;
        this.transaction = transaction;
    }

    /**
     * A context opens before its game plays, so every record already in a source when it opens belongs to an earlier
     * context or to none: the fence is captured first, and a failed capture opens nothing. This covers a replay of a
     * closed case, whose records carry the same game, and a backlog left before the first context of a deployment.
     */
    @Override
    public ExecutionCheckpoint handle(ExecutionContext context) {
        controlLock.lock();
        try {
            if (executions.find().isEmpty()) {
                eventFence.capture();
            }
            return assembler.assemble(Objects.requireNonNull(transaction.execute(status -> register(context))));
        } finally {
            controlLock.unlock();
        }
    }

    private Execution register(ExecutionContext context) {
        var existing = executions.findWithLock();
        if (existing.isPresent()) {
            return requireSameContext(existing.get(), context);
        }
        if (!gameplay.stream().allMatch(GameplayState::isEmpty)) {
            throw new ExecutionStateNotCleanException("The service still holds gameplay state of an earlier context");
        }
        var created = Execution.configure(context);
        if (executions.insertIfAbsent(created)) {
            // A replay hosts the game of the case's earlier play again; the fence keeps that play's records out.
            executions.forgetClosedGames(context.caseKey());
            return created;
        }
        return requireSameContext(executions.findWithLock().orElseThrow(), context);
    }

    private static Execution requireSameContext(Execution existing, ExecutionContext context) {
        if (!existing.context().equals(context)) {
            throw new ExecutionContextConflictException("A different execution context is already configured");
        }
        return existing;
    }
}
