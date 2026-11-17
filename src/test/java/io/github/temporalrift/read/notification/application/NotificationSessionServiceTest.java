package io.github.temporalrift.read.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.notification.domain.model.NotificationMessage;
import io.github.temporalrift.read.notification.domain.model.NotificationRecipient;
import io.github.temporalrift.read.notification.domain.model.NotificationSession;
import io.github.temporalrift.read.notification.domain.model.NotificationSessionRegistry;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.Phase;

class NotificationSessionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationSessionRegistry registry = mock(NotificationSessionRegistry.class);
    private final GetPlayerGameStateUseCase playerGameState = mock(GetPlayerGameStateUseCase.class);

    @Test
    void aNonPositiveBufferLimitIsRejected() {
        assertThatThrownBy(() -> service(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectRegistersTheSessionAndSendsItsSnapshot() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var delivery = mock(NotificationDeliveryPort.class);
        var snapshot = new GetPlayerGameStateUseCase.Result(
                gameId, 1, Phase.LOBBY, "ERASERS", List.of(), null, List.of(), 0, List.of(), List.of());
        given(playerGameState.get(gameId, playerId)).willReturn(snapshot);

        service(4).connect("session", gameId, playerId, delivery);

        var session = ArgumentCaptor.forClass(NotificationSession.class);
        verify(registry).register(session.capture());
        assertThat(session.getValue().sessionId()).isEqualTo("session");
        assertThat(session.getValue().recipient()).isEqualTo(new NotificationRecipient(gameId, playerId));

        var message = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(delivery).send(message.capture());
        assertThat(message.getValue().type()).isEqualTo("SNAPSHOT");
        assertThat(message.getValue().payload()).isEqualTo(objectMapper.valueToTree(snapshot));
    }

    @Test
    void aFailedSnapshotUnregistersTheSessionAndClosesItsDelivery() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var delivery = mock(NotificationDeliveryPort.class);
        given(playerGameState.get(gameId, playerId)).willThrow(new IllegalStateException("gone"));

        assertThatThrownBy(() -> service(4).connect("session", gameId, playerId, delivery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("gone");

        verify(registry).unregister("session");
        verify(delivery).close();
        verify(delivery, never()).send(any());
    }

    @Test
    void disconnectUnregistersTheSession() {
        service(4).disconnect("session");

        verify(registry).unregister("session");
    }

    private NotificationSessionService service(int maxPendingMessages) {
        return new NotificationSessionService(playerGameState, registry, objectMapper, maxPendingMessages);
    }
}
