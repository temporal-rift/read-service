package io.github.temporalrift.read.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

class CurrentPlayerTest {

    @Test
    void resolvesAuthenticatedWebSocketPrincipal() {
        var playerId = UUID.randomUUID();

        var result = CurrentPlayer.id(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)));

        assertThat(result).isEqualTo(playerId);
    }
}
