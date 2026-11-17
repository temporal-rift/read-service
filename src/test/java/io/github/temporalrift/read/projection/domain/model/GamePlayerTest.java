package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GamePlayerTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Test
    void aNegativeSeatIsRejected() {
        assertThatThrownBy(() -> new GamePlayer(PLAYER_ID, 0, true, "ERASERS", "Ada", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("seatIndex must not be negative");
    }

    @Test
    void aPlayerMayHaveNoSeatOrNameYet() {
        var player = new GamePlayer(PLAYER_ID, 0, false, "ERASERS");

        assertThat(player.seatIndex()).isNull();
        assertThat(player.playerName()).isNull();
        assertThat(new GamePlayer(PLAYER_ID, 0, false, "ERASERS", "Ada").seatIndex())
                .isNull();
    }

    @Test
    void withScoreKeepsEveryOtherField() {
        var player = new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Ada", 2);

        assertThat(player.withScore(9)).isEqualTo(new GamePlayer(PLAYER_ID, 9, true, "ERASERS", "Ada", 2));
    }

    @Test
    void withConnectedKeepsEveryOtherField() {
        var player = new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Ada", 2);

        assertThat(player.withConnected(false)).isEqualTo(new GamePlayer(PLAYER_ID, 3, false, "ERASERS", "Ada", 2));
    }

    @Test
    void withFactionKeepsEveryOtherField() {
        var player = new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Ada", 2);

        assertThat(player.withFaction("WEAVERS")).isEqualTo(new GamePlayer(PLAYER_ID, 3, true, "WEAVERS", "Ada", 2));
    }

    @Test
    void withPlayerNameKeepsEveryOtherField() {
        var player = new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Ada", 2);

        assertThat(player.withPlayerName("Grace")).isEqualTo(new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Grace", 2));
    }

    @Test
    void withSeatIndexKeepsEveryOtherField() {
        var player = new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Ada", 2);

        assertThat(player.withSeatIndex(4)).isEqualTo(new GamePlayer(PLAYER_ID, 3, true, "ERASERS", "Ada", 4));
    }
}
