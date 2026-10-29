package io.github.temporalrift.read.simulation.domain.execution;

import java.util.UUID;

/** A seat of the case; read-service only compares it, so the faction stays the contract's name. */
public record SimulationSeat(int seatIndex, UUID playerId, String faction) {}
