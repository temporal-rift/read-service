package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.time.Instant;
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

import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;

@Entity
@Table(name = "player_game_state")
class PlayerGameStateEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "my_faction")
    private String myFaction;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_game_state_hand_card", joinColumns = @JoinColumn(name = "player_game_state_id"))
    @OrderColumn(name = "card_position")
    private List<PlayerGameStateHandCardValue> hand;

    @Column(name = "pending_hand_selection_expires_at")
    private Instant pendingHandSelectionExpiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "player_game_state_pending_hand_card",
            joinColumns = @JoinColumn(name = "player_game_state_id"))
    @OrderColumn(name = "card_position")
    private List<PlayerGameStatePendingHandCardValue> pendingHandSelectionCards;

    protected PlayerGameStateEntity() {}

    PlayerGameStateEntity(
            UUID id,
            UUID gameId,
            UUID playerId,
            String myFaction,
            List<PlayerGameStateHandCardValue> hand,
            Instant pendingHandSelectionExpiresAt,
            List<PlayerGameStatePendingHandCardValue> pendingHandSelectionCards) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.myFaction = myFaction;
        this.hand = hand;
        this.pendingHandSelectionExpiresAt = pendingHandSelectionExpiresAt;
        this.pendingHandSelectionCards = pendingHandSelectionCards;
    }

    static PlayerGameStateEntity fromDomain(UUID id, PlayerGameState domain) {
        return new PlayerGameStateEntity(
                id,
                domain.gameId(),
                domain.playerId(),
                domain.myFaction(),
                domain.myHand().stream()
                        .map(PlayerGameStateHandCardValue::fromDomain)
                        .toList(),
                domain.pendingHandSelection() == null
                        ? null
                        : domain.pendingHandSelection().expiresAt(),
                domain.pendingHandSelection() == null
                        ? List.of()
                        : domain.pendingHandSelection().cards().stream()
                                .map(PlayerGameStatePendingHandCardValue::fromDomain)
                                .toList());
    }

    PlayerGameState toDomain() {
        return new PlayerGameState(
                gameId,
                playerId,
                myFaction,
                hand.stream().map(PlayerGameStateHandCardValue::toDomain).toList(),
                pendingHandSelectionExpiresAt == null
                        ? null
                        : new PendingHandSelection(
                                pendingHandSelectionCards.stream()
                                        .map(PlayerGameStatePendingHandCardValue::toDomain)
                                        .toList(),
                                pendingHandSelectionExpiresAt));
    }

    UUID getGameId() {
        return gameId;
    }

    UUID getPlayerId() {
        return playerId;
    }

    void setMyFaction(String myFaction) {
        this.myFaction = myFaction;
    }

    void setHand(List<PlayerGameStateHandCardValue> hand) {
        this.hand = hand;
    }

    void setPendingHandSelection(PendingHandSelection pendingHandSelection) {
        pendingHandSelectionExpiresAt = pendingHandSelection == null ? null : pendingHandSelection.expiresAt();
        pendingHandSelectionCards = pendingHandSelection == null
                ? List.of()
                : pendingHandSelection.cards().stream()
                        .map(PlayerGameStatePendingHandCardValue::fromDomain)
                        .toList();
    }
}
