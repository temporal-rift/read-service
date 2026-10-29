package io.github.temporalrift.read.simulation.domain.execution;

import java.util.UUID;

/** The open context's case and the game it hosts, or a null {@code gameId} before it handled a record. */
public record ExecutionScope(UUID caseKey, UUID gameId) {}
