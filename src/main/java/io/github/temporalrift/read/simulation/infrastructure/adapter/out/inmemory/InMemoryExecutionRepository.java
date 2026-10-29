package io.github.temporalrift.read.simulation.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionScope;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

/**
 * The in-memory store has exclusive row locks only, so record handlers hold the execution exclusively and run one at a
 * time; a close still waits for the handler in progress, as it does on PostgreSQL.
 */
@Repository
@Profile("in-memory")
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class InMemoryExecutionRepository implements ExecutionRepository {

    private static final String PARTITION = "simulation";
    private static final RowKey OPEN = RowKey.of("open");
    private static final RowKey LAST_CLOSED = RowKey.of("last-closed");

    private final Table<String, Execution> executions;
    private final Table<String, UUID> closedExecution;
    private final Table<String, UUID> closedGames;

    InMemoryExecutionRepository(InMemoryDatabase database) {
        this.executions = database.table("simulation_execution", false);
        this.closedExecution = database.table("simulation_closed_execution", false);
        this.closedGames = database.table("simulation_closed_game", false);
    }

    @Override
    public Optional<Execution> find() {
        return executions.find(PARTITION, OPEN);
    }

    @Override
    public Optional<Execution> findWithLock() {
        executions.lock(PARTITION, OPEN);
        return find();
    }

    @Override
    public boolean insertIfAbsent(Execution execution) {
        executions.lock(PARTITION, OPEN);
        return executions.putIfAbsent(PARTITION, OPEN, execution);
    }

    @Override
    public void update(Execution execution) {
        findWithLock()
                .ifPresent(stored -> executions.put(
                        PARTITION,
                        OPEN,
                        new Execution(
                                stored.context(), execution.revision(), execution.logicalTime(), stored.gameId())));
    }

    @Override
    public Optional<ExecutionScope> findScope() {
        return find().map(InMemoryExecutionRepository::scopeOf);
    }

    @Override
    public Optional<ExecutionScope> findScopeForHandling() {
        return findWithLock().map(InMemoryExecutionRepository::scopeOf);
    }

    @Override
    public void bindGame(UUID caseKey, UUID gameId) {
        findWithLock()
                .filter(open -> open.context().caseKey().equals(caseKey) && open.gameId() == null)
                .ifPresent(open -> executions.put(
                        PARTITION, OPEN, new Execution(open.context(), open.revision(), open.logicalTime(), gameId)));
    }

    @Override
    public void close(Execution execution) {
        var caseKey = execution.context().caseKey();
        executions.remove(PARTITION, OPEN);
        closedExecution.put(PARTITION, LAST_CLOSED, caseKey);
        if (execution.gameId() != null) {
            closedGames.put(PARTITION, RowKey.of(execution.gameId()), caseKey);
        }
    }

    @Override
    public Optional<UUID> lastClosedCaseKey() {
        return closedExecution.find(PARTITION, LAST_CLOSED);
    }

    @Override
    public boolean isClosedGame(UUID gameId) {
        return closedGames.find(PARTITION, RowKey.of(gameId)).isPresent();
    }

    @Override
    public void forgetClosedGames(UUID caseKey) {
        closedGames.removeIf(PARTITION, caseKey::equals);
    }

    private static ExecutionScope scopeOf(Execution execution) {
        return new ExecutionScope(execution.context().caseKey(), execution.gameId());
    }
}
