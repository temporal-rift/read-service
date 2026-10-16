package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;

public abstract class GamePlayerContract extends PortContract {

    protected abstract GamePlayerRepository port();

    @Test
    void aSavedPlayerIsFoundByGameAndPlayer() {
        var player = new GamePlayer(UUID.randomUUID(), 3, true, "ERASERS", "Ada");

        inTransaction(() -> port().save(gameId, player));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, player.playerId())))
                .contains(player);
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(otherGameId, player.playerId())))
                .isEmpty();
    }

    @Test
    void savingAgainReplacesScoreConnectionFactionAndName() {
        var playerId = UUID.randomUUID();
        inTransaction(() -> port().save(gameId, new GamePlayer(playerId, 0, true, null, "Ada")));

        inTransaction(() -> port().save(gameId, new GamePlayer(playerId, 7, false, "WEAVERS", "Ada L.")));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .containsExactly(new GamePlayer(playerId, 7, false, "WEAVERS", "Ada L."));
    }

    @Test
    void playersOfAGameAreListedInTheOrderTheyWereFirstSaved() {
        var first = new GamePlayer(HIGH, 0, true, null, "First");
        var second = new GamePlayer(LOW, 0, true, null, "Second");
        inTransaction(() -> {
            port().save(gameId, first);
            port().save(gameId, second);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(first, second);
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isEmpty();
    }
}
