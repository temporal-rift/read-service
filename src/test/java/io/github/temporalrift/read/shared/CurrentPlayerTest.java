package io.github.temporalrift.read.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

class CurrentPlayerTest {

    @Test
    void resolvesAuthenticatedWebSocketPrincipal() {
        var playerId = UUID.randomUUID();

        var result = CurrentPlayer.id(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)));

        assertThat(result).isEqualTo(playerId);
    }

    @Test
    void rejectsUnauthenticatedWebSocketPrincipal() {
        var principal = new PlayerPrincipal(UUID.randomUUID());
        var unauthenticated = UsernamePasswordAuthenticationToken.unauthenticated(principal, "credentials");

        assertThatThrownBy(() -> CurrentPlayer.id(unauthenticated)).isInstanceOf(AccessDeniedException.class);
    }
}
