package io.github.temporalrift.read.simulation.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.simulation.domain.execution.ClockOperation;
import io.github.temporalrift.read.simulation.domain.port.out.ClockOperationRepository;

@Repository
@Profile("in-memory")
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class InMemoryClockOperationRepository implements ClockOperationRepository {

    private static final String PARTITION = "simulation";

    private final Table<String, ClockOperation> operations;

    InMemoryClockOperationRepository(InMemoryDatabase database) {
        this.operations = database.table("simulation_clock_operation", false);
    }

    @Override
    public Optional<ClockOperation> find(UUID operationId) {
        return operations.find(PARTITION, RowKey.of(operationId));
    }

    @Override
    public void save(ClockOperation operation) {
        operations.put(PARTITION, RowKey.of(operation.request().operationId()), operation);
    }

    @Override
    public void clear() {
        operations.clear();
    }
}
