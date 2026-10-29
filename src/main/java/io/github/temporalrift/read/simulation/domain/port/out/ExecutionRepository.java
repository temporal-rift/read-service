package io.github.temporalrift.read.simulation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionScope;

public interface ExecutionRepository {

    Optional<Execution> find();

    /** Reads the execution holding a write lock until the surrounding transaction ends. */
    Optional<Execution> findWithLock();

    /** Returns whether this call stored the execution; {@code false} means one was already configured. */
    boolean insertIfAbsent(Execution execution);

    /** Stores the execution's revision and logical time. */
    void update(Execution execution);

    /** Reads the open context's case and game without holding a lock. */
    Optional<ExecutionScope> findScope();

    /**
     * Reads the open context's case and game holding a lock until the surrounding transaction ends, which a close
     * waits for; record handlers do not wait for each other's.
     */
    Optional<ExecutionScope> findScopeForHandling();

    /** Binds the open context of {@code caseKey} to {@code gameId}, unless it is closed or already hosts a game. */
    void bindGame(UUID caseKey, UUID gameId);

    /**
     * Removes the execution, remembers its case as the most recently closed one and its game, if any, as a game of a
     * closed context.
     */
    void close(Execution execution);

    /** The case of the context closed last, which stays known while later contexts come and go. */
    Optional<UUID> lastClosedCaseKey();

    /** Whether {@code gameId} was the game of a context that is closed and not opened again. */
    boolean isClosedGame(UUID gameId);

    /** Forgets the games of {@code caseKey}'s closed contexts, so a replay of the case can host its game again. */
    void forgetClosedGames(UUID caseKey);
}
