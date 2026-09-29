package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** {@code initialProbability} is the event card's printed starting weight, never a live probability. */
public record EventOutcome(UUID outcomeId, String description, int initialProbability) {}
