package io.github.temporalrift.read.notification.infrastructure.adapter.in.kafka;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.notification.application.port.in.FanOutNotificationUseCase;
import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.InboundEventAdmission;

@Component
class NotificationKafkaConsumer {

    private final InboundEventAdmission admission;
    private final FanOutNotificationUseCase fanOut;

    NotificationKafkaConsumer(InboundEventAdmission admission, FanOutNotificationUseCase fanOut) {
        this.admission = admission;
        this.fanOut = fanOut;
    }

    @KafkaListener(topics = "game.events", groupId = "read-service.notification-game-events")
    @Transactional(propagation = REQUIRES_NEW)
    void handleGameEvent(Message<Object> message) {
        handle(message, "notification.game-events");
    }

    @KafkaListener(topics = "timeline.events", groupId = "read-service.notification-timeline-events")
    @Transactional(propagation = REQUIRES_NEW)
    void handleTimelineEvent(Message<Object> message) {
        handle(message, "notification.timeline-events");
    }

    private void handle(Message<Object> message, String consumer) {
        admission.admit(message, consumer).ifPresent(ignored -> fanOut.fanOut(message));
    }
}
