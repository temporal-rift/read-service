package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    protected GameChainEntity() {}

    GameChainEntity(UUID gameId, UUID chainId, ChainStatus status, int length) {
        this.gameId = gameId;
        this.chainId = chainId;
        this.status = status.name();
        this.length = length;
    }

    static GameChainEntity fromDomain(GameChain domain) {
        return new GameChainEntity(domain.gameId(), domain.chainId(), domain.status(), domain.length());
    }

    GameChain toDomain() {
        return new GameChain(gameId, chainId, ChainStatus.valueOf(status), length);
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
}
