package io.github.temporalrift.read.projection.domain.model;

import java.util.List;

public record LastRoundSummary(int roundNumber, List<RoundActionSummary> actionSummaries) {

    public LastRoundSummary {
        actionSummaries = List.copyOf(actionSummaries);
    }
}
