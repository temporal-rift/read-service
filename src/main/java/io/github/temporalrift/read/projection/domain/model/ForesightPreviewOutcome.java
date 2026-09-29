package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** {@code initialProbability} is the previewed card's printed starting weight, never a live probability. */
public record ForesightPreviewOutcome(UUID catalogOutcomeId, String description, int initialProbability) {}
