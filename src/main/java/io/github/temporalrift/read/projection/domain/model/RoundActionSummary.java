package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

public record RoundActionSummary(UUID playerId, String actionCategory, String actionFamily, boolean skipped) {}
