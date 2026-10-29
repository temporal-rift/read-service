package io.github.temporalrift.read.simulation.application.query;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.shared.GameEndProbe;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionCheckpoint;
import io.github.temporalrift.read.simulation.domain.port.out.SourceWatermarks;

/**
 * read-service has no outbox, continuations or timers, so a record's work is done once a group's position passed it:
 * every checkpoint is drained up to its watermarks.
 */
@Component
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
public class ExecutionCheckpointAssembler {

    private final GameEndProbe gameEnd;
    private final SourceWatermarks watermarks;

    ExecutionCheckpointAssembler(GameEndProbe gameEnd, SourceWatermarks watermarks) {
        this.gameEnd = gameEnd;
        this.watermarks = watermarks;
    }

    public ExecutionCheckpoint assemble(Execution execution) {
        var gameId = execution.gameId();
        return new ExecutionCheckpoint(
                execution.context().caseKey(),
                execution.context().manifestDigest(),
                execution.context().rulesDigest(),
                execution.revision(),
                execution.logicalTime(),
                gameId,
                execution.state(gameId != null && gameEnd.hasEnded(gameId)),
                true,
                0,
                0,
                0,
                null,
                watermarks.current());
    }
}
