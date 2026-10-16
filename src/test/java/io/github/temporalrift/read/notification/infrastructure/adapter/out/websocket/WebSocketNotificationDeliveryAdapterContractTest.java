package io.github.temporalrift.read.notification.infrastructure.adapter.out.websocket;

import static org.mockito.Mockito.mock;

import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.contract.NotificationDeliveryContract;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

class WebSocketNotificationDeliveryAdapterContractTest extends NotificationDeliveryContract {

    @Override
    protected NotificationDeliveryPort delivery() {
        return new WebSocketNotificationDeliveryAdapter(mock(WebSocketSession.class), new ObjectMapper());
    }
}
