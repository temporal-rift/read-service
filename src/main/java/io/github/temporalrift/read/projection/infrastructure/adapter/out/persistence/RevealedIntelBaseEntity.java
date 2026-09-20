package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/** Shared game/viewer/era/event identity for the per-kind revealed-intel tables. */
@MappedSuperclass
abstract class RevealedIntelBaseEntity extends PlayerScopedBaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "observed_in_round", nullable = false)
    private int observedInRound;

    protected RevealedIntelBaseEntity() {}

    protected RevealedIntelBaseEntity(
            UUID id, UUID gameId, UUID playerId, int eraNumber, UUID eventId, int observedInRound) {
        super(id, gameId, playerId, eraNumber);
        this.eventId = eventId;
        this.observedInRound = observedInRound;
    }

    UUID getEventId() {
        return eventId;
    }

    int getObservedInRound() {
        return observedInRound;
    }

    void setObservedInRound(int observedInRound) {
        this.observedInRound = observedInRound;
    }
}
