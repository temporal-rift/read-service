package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

public record ForesightPreviewEvent(UUID catalogEventId, String title, List<ForesightPreviewOutcome> outcomes) {

    public ForesightPreviewEvent {
        outcomes = List.copyOf(outcomes);
    }
}
