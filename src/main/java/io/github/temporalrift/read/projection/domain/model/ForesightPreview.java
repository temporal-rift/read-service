package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * A foretelling viewer's private preview of the next era's freshly-drawn events, in deck order, recorded for the era
 * in which it was revealed. {@code emptyReason} is set only when there is nothing to preview.
 */
public record ForesightPreview(
        UUID gameId,
        UUID playerId,
        int eraNumber,
        int nextEraNumber,
        List<ForesightPreviewEvent> revealedEvents,
        String emptyReason) {

    public ForesightPreview {
        revealedEvents = List.copyOf(revealedEvents);
    }
}
