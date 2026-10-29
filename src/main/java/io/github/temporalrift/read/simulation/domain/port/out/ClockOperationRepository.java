package io.github.temporalrift.read.simulation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.simulation.domain.execution.ClockOperation;

public interface ClockOperationRepository {

    Optional<ClockOperation> find(UUID operationId);

    void save(ClockOperation operation);

    void clear();
}
