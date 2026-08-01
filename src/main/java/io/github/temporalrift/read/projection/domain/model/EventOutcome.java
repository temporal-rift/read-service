package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

public record EventOutcome(UUID outcomeId, String description) {}
