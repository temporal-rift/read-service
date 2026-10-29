package io.github.temporalrift.read.simulation.domain.execution;

import java.time.Instant;
import java.util.UUID;

/**
 * The open execution of one isolated case: its pinned context, service-owned revision and logical time, and the game
 * it hosts once read-service handled a record of it.
 */
public record Execution(ExecutionContext context, long revision, Instant logicalTime, UUID gameId) {

    public static Execution configure(ExecutionContext context) {
        return new Execution(context, 0, context.logicalTime(), null);
    }

    /** Applies a clock advance, or returns the operation recorded earlier for the same identifier. */
    public Advance advance(ClockAdvance request, ClockOperation recorded) {
        if (recorded != null) {
            if (!recorded.request().equals(request)) {
                throw new IdempotencyConflictException(request.operationId());
            }
            return new Advance(this, recorded, false);
        }
        if (request.expectedRevision() != revision) {
            throw new StaleExecutionRevisionException(request.expectedRevision(), revision);
        }
        if (request.targetTime().isBefore(logicalTime)) {
            throw new ClockRegressionException(request.targetTime(), logicalTime);
        }
        var advanced = new Execution(context, revision + 1, request.targetTime(), gameId);
        var operation = new ClockOperation(
                request, new ClockAcknowledgement(request.operationId(), advanced.revision, advanced.logicalTime));
        return new Advance(advanced, operation, true);
    }

    /** {@code READY} until a record of the game was handled, then {@code ACTIVE} until its end was applied. */
    public ExecutionState state(boolean gameEnded) {
        if (gameId == null) {
            return ExecutionState.READY;
        }
        return gameEnded ? ExecutionState.TERMINAL : ExecutionState.ACTIVE;
    }

    public record Advance(Execution execution, ClockOperation operation, boolean applied) {}
}
