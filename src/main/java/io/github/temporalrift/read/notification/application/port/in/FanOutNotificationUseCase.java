package io.github.temporalrift.read.notification.application.port.in;

import org.springframework.messaging.Message;

public interface FanOutNotificationUseCase {

    void fanOut(Message<Object> message);
}
