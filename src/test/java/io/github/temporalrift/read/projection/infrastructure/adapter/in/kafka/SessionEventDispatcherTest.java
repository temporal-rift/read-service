package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventHeaders;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerJoinedLobbyPayload;

@ExtendWith(MockitoExtension.class)
class SessionEventDispatcherTest {

    @Mock
    ProjectionEventApplier applier;

    @Test
    void onPlayerJoinedLobby_appliesTheJoinUnderTheHeaderGameId() {
        var gameId = UUID.randomUUID();
        var payload = new PlayerJoinedLobbyPayload(UUID.randomUUID(), UUID.randomUUID(), "Ada");
        var headers =
                new EventHeaders("PlayerJoinedLobby", UUID.randomUUID(), gameId, "Game", gameId, Instant.now(), 1);

        new SessionEventDispatcher(applier).onPlayerJoinedLobby(payload, headers);

        then(applier).should().applyPlayerJoinedLobby(payload, gameId);
    }
}
