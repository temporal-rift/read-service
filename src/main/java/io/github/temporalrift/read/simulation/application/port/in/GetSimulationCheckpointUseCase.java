package io.github.temporalrift.read.simulation.application.port.in;

import io.github.temporalrift.read.simulation.domain.execution.ExecutionCheckpoint;

public interface GetSimulationCheckpointUseCase {

    ExecutionCheckpoint handle();
}
