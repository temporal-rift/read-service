package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** One exact outcome state privately revealed to a Scan viewer. */
public record RevealedProbabilityOutcome(UUID outcomeId, int probability, boolean isAnnihilated, boolean isSealed) {}
