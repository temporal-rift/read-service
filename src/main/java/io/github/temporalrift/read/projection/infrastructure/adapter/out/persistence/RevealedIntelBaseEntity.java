package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** Shared game/viewer/era/event identity for the per-kind revealed-intel tables. */
@MappedSuperclass
abstract class RevealedIntelBaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "observed_in_round", nullable = false)
    private int observedInRound;

    protected RevealedIntelBaseEntity() {}

    protected RevealedIntelBaseEntity(
            UUID id, UUID gameId, UUID playerId, int eraNumber, UUID eventId, int observedInRound) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.eraNumber = eraNumber;
        this.eventId = eventId;
        this.observedInRound = observedInRound;
    }

    UUID getGameId() {
        return gameId;
    }

    UUID getPlayerId() {
        return playerId;
    }

    int getEraNumber() {
        return eraNumber;
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
