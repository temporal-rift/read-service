package io.github.temporalrift.read.simulation.application.port.in;

import java.util.UUID;

public interface CloseSimulationExecutionUseCase {

    void handle(UUID caseKey);
}
