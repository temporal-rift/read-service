package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import io.github.temporalrift.read.projection.domain.model.RoundActionSummary;

@Embeddable
record LastRoundSummaryActionValue(
        @Column(name = "player_id", nullable = false) UUID playerId,
        // Nullable: a skipped player played no card, so RoundSummaryPublished legitimately omits these for
        // that entry (only playerId/skipped are required in the event contract).
        @Column(name = "action_category") String actionCategory,
        @Column(name = "action_family") String actionFamily,
        @Column(name = "skipped", nullable = false) boolean skipped) {

    static LastRoundSummaryActionValue fromDomain(RoundActionSummary summary) {
        return new LastRoundSummaryActionValue(
                summary.playerId(), summary.actionCategory(), summary.actionFamily(), summary.skipped());
    }

    RoundActionSummary toDomain() {
        return new RoundActionSummary(playerId, actionCategory, actionFamily, skipped);
    }
}
