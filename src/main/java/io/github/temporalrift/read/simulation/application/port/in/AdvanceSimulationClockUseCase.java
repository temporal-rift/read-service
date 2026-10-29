package io.github.temporalrift.read.simulation.application.port.in;

import io.github.temporalrift.read.simulation.domain.execution.ClockAcknowledgement;
import io.github.temporalrift.read.simulation.domain.execution.ClockAdvance;

public interface AdvanceSimulationClockUseCase {

    ClockAcknowledgement handle(ClockAdvance advance);
}
