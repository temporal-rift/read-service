package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import io.github.temporalrift.read.projection.domain.model.EventOutcome;

@Embeddable
record GameActiveEventOutcomeValue(
        @Column(name = "outcome_id", nullable = false) UUID outcomeId,
        @Column(name = "description", nullable = false) String description) {

    static GameActiveEventOutcomeValue fromDomain(EventOutcome outcome) {
        return new GameActiveEventOutcomeValue(outcome.outcomeId(), outcome.description());
    }

    EventOutcome toDomain() {
        return new EventOutcome(outcomeId, description);
    }
}
