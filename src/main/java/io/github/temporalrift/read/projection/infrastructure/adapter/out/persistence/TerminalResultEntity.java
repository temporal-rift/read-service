package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_terminal_result")
class TerminalResultEntity {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "end_reason")
    private String endReason;

    protected TerminalResultEntity() {}

    TerminalResultEntity(UUID gameId) {
        this.gameId = gameId;
    }

    UUID getGameId() {
        return gameId;
    }

    String getEndReason() {
        return endReason;
    }

    void setEndReason(String endReason) {
        this.endReason = endReason;
    }
}
