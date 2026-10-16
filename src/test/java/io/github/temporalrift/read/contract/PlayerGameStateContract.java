package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.PendingHandCard;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;

public abstract class PlayerGameStateContract extends PortContract {

    private final UUID playerId = UUID.randomUUID();

    protected abstract PlayerGameStateRepository port();

    @Test
    void anUnknownPlayerHasNoState() {
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId)))
                .isEmpty();
    }

    @Test
    void aSavedStateIsReadBackWithHandPendingSelectionAndJam() {
        var state = new PlayerGameState(
                gameId,
                playerId,
                "ERASERS",
                List.of(new HandCard(UUID.randomUUID(), "PUSH", "II"), new HandCard(UUID.randomUUID(), "SCAN", "I")),
                new PendingHandSelection(
                        List.of(new PendingHandCard(UUID.randomUUID(), "TRACE", "I", 3)),
                        Instant.parse("2026-08-15T00:00:00Z")),
                2,
                3);

        inTransaction(() -> port().save(state));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId)))
                .contains(state);
    }

    @Test
    void savingAgainReplacesTheStoredState() {
        inTransaction(() -> port().save(state("ERASERS", List.of(new HandCard(UUID.randomUUID(), "PUSH")))));

        inTransaction(() -> port().save(state("WEAVERS", List.of())));

        var read = inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId))
                .orElseThrow();
        assertThat(read.myFaction()).isEqualTo("WEAVERS");
        assertThat(read.myHand()).isEmpty();
    }

    @Test
    void statesAreScopedToTheirGameAndPlayer() {
        inTransaction(() -> port().save(state("ERASERS", List.of())));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(otherGameId, playerId)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, UUID.randomUUID())))
                .isEmpty();
    }

    @Test
    void anUpdateOfATransactionThatThrowsIsAbsentAfterwards() {
        inTransaction(() -> port().save(state("ERASERS", List.of())));

        assertThatThrownBy(() -> inTransaction(() -> {
                    port().save(state("WEAVERS", List.of(new HandCard(UUID.randomUUID(), "PUSH"))));
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        var read = inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId))
                .orElseThrow();
        assertThat(read.myFaction()).isEqualTo("ERASERS");
        assertThat(read.myHand()).isEmpty();
    }

    private PlayerGameState state(String faction, List<HandCard> hand) {
        return new PlayerGameState(gameId, playerId, faction, hand, null, null, null);
    }
}
