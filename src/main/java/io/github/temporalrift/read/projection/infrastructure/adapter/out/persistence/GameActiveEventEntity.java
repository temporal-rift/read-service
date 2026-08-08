package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import io.github.temporalrift.read.projection.domain.model.CarryOverState;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;

@Entity
@Table(name = "game_active_event")
class GameActiveEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "carry_over_state", nullable = false)
    private String carryOverState;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_active_event_outcome", joinColumns = @JoinColumn(name = "game_active_event_id"))
    @OrderColumn(name = "outcome_position")
    private List<GameActiveEventOutcomeValue> outcomes;

    protected GameActiveEventEntity() {}

    GameActiveEventEntity(
            UUID id,
            UUID gameId,
            UUID eventId,
            String title,
            String carryOverState,
            List<GameActiveEventOutcomeValue> outcomes) {
        this.id = id;
        this.gameId = gameId;
        this.eventId = eventId;
        this.title = title;
        this.carryOverState = carryOverState;
        this.outcomes = outcomes;
    }

    static GameActiveEventEntity fromDomain(UUID id, UUID gameId, GameActiveEvent domain) {
        return new GameActiveEventEntity(
                id,
                gameId,
                domain.eventId(),
                domain.title(),
                domain.carryOverState().name(),
                domain.outcomes().stream()
                        .map(GameActiveEventOutcomeValue::fromDomain)
                        .toList());
    }

    GameActiveEvent toDomain() {
        return new GameActiveEvent(
                eventId,
                title,
                CarryOverState.valueOf(carryOverState),
                outcomes.stream().map(GameActiveEventOutcomeValue::toDomain).toList());
    }

    UUID getGameId() {
        return gameId;
    }

    UUID getEventId() {
        return eventId;
    }
}
