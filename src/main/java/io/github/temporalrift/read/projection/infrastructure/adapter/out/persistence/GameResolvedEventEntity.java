package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "game_resolved_event",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_resolved_event_game_id_event_id",
                        columnNames = {"game_id", "event_id"}))
class GameResolvedEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    protected GameResolvedEventEntity() {}
}
