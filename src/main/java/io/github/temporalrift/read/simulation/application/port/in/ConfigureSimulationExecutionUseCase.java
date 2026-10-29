package io.github.temporalrift.read.simulation.application.port.in;

import io.github.temporalrift.read.simulation.domain.execution.ExecutionCheckpoint;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;

public interface ConfigureSimulationExecutionUseCase {

    ExecutionCheckpoint handle(ExecutionContext context);
}
