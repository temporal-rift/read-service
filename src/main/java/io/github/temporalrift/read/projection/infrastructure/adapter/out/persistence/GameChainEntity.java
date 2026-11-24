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

import io.github.temporalrift.read.projection.domain.model.ChainStatus;
import io.github.temporalrift.read.projection.domain.model.GameChain;

@Entity
@Table(name = "game_chain_projection")
class GameChainEntity {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "chain_id", nullable = false)
    private UUID chainId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "length", nullable = false)
    private int length;

    @Column(name = "pending_event_id")
    private UUID pendingEventId;

    @Column(name = "pending_outcome_id")
    private UUID pendingOutcomeId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_chain_confirmed_event", joinColumns = @JoinColumn(name = "game_chain_id"))
    @Column(name = "event_id", nullable = false)
    @OrderColumn(name = "event_position")
    private List<UUID> confirmedEventIds;

    protected GameChainEntity() {}

    GameChainEntity(
            UUID gameId,
            UUID chainId,
            ChainStatus status,
            int length,
            GameChain.PendingLink pendingLink,
            List<UUID> confirmedEventIds) {
        this.gameId = gameId;
        this.chainId = chainId;
        this.status = status.name();
        this.length = length;
        setPendingLink(pendingLink);
        setConfirmedEventIds(confirmedEventIds);
    }

    static GameChainEntity fromDomain(GameChain domain) {
        return new GameChainEntity(
                domain.gameId(),
                domain.chainId(),
                domain.status(),
                domain.length(),
                domain.pendingLink(),
                domain.confirmedEventIds());
    }

    GameChain toDomain() {
        var pending = pendingEventId == null ? null : new GameChain.PendingLink(pendingEventId, pendingOutcomeId);
        return new GameChain(
                gameId,
                chainId,
                ChainStatus.valueOf(status),
                length,
                pending,
                confirmedEventIds == null ? List.of() : List.copyOf(confirmedEventIds));
    }

    void setChainId(UUID chainId) {
        this.chainId = chainId;
    }

    void setStatus(ChainStatus status) {
        this.status = status.name();
    }

    void setLength(int length) {
        this.length = length;
    }

    void setPendingLink(GameChain.PendingLink pendingLink) {
        this.pendingEventId = pendingLink == null ? null : pendingLink.eventId();
        this.pendingOutcomeId = pendingLink == null ? null : pendingLink.outcomeId();
    }

    void setConfirmedEventIds(List<UUID> confirmedEventIds) {
        this.confirmedEventIds = List.copyOf(confirmedEventIds);
    }
}
