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
    void savingAgainReplacesScoreConnectionFactionNameAndSeat() {
        var playerId = UUID.randomUUID();
        inTransaction(() -> port().save(gameId, new GamePlayer(playerId, 0, true, null, null)));

        inTransaction(() -> port().save(gameId, new GamePlayer(playerId, 7, false, "WEAVERS", "Ada L.", 2)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .containsExactly(new GamePlayer(playerId, 7, false, "WEAVERS", "Ada L.", 2));
    }

    @Test
    void playersAreListedInSeatOrderWhateverOrderTheyWereSavedIn() {
        var cy = seated(0, "Cy");
        var ada = seated(1, "Ada");
        var ben = seated(2, "Ben");

        inTransaction(() -> {
            port().save(gameId, ben);
            port().save(gameId, cy);
            port().save(gameId, ada);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(cy, ada, ben);
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isEmpty();
    }

    @Test
    void seatOrderDoesNotDependOnWhatTheStoreHeldBefore() {
        // Rows of another game come and go between this game's saves, freeing space a later row may take.
        var cy = seated(0, "Cy");
        var ada = seated(1, "Ada");
        var ben = seated(2, "Ben");
        inTransaction(() -> port().save(otherGameId, seated(0, "Transient")));
        inTransaction(() -> port().save(gameId, ada));
        inTransaction(() -> port().save(otherGameId, seated(1, "Transient")));
        inTransaction(() -> port().save(gameId, ben));
        inTransaction(() -> port().save(gameId, cy));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(cy, ada, ben);
    }

    @Test
    void playersNotYetSeatedComeLastOrderedByPlayerId() {
        var seated = seated(0, "Seated");
        var high = new GamePlayer(HIGH, 0, true, null, null);
        var low = new GamePlayer(LOW, 0, true, null, null);
        inTransaction(() -> {
            port().save(gameId, high);
            port().save(gameId, low);
            port().save(gameId, seated);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(seated, low, high);
    }

    private static GamePlayer seated(int seat, String name) {
        return new GamePlayer(UUID.randomUUID(), 0, true, null, name, seat);
    }
}
