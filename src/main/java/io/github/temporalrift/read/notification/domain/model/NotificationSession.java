package io.github.temporalrift.read.notification.domain.model;

import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

public final class NotificationSession {
    private final String sessionId;
    private final NotificationRecipient recipient;
    private final NotificationDeliveryPort delivery;
    private final java.util.Queue<NotificationMessage> pending = new java.util.ArrayDeque<>();
    private boolean initializing = true;

    public NotificationSession(String sessionId, NotificationRecipient recipient, NotificationDeliveryPort delivery) {
        this.sessionId = sessionId;
        this.recipient = recipient;
        this.delivery = delivery;
    }

    public String sessionId() {
        return sessionId;
    }

    public NotificationRecipient recipient() {
        return recipient;
    }

    public synchronized void deliver(NotificationMessage message) {
        if (initializing) {
            pending.add(message);
        } else {
            delivery.send(message);
        }
    }

    public synchronized void activate(NotificationMessage snapshot) {
        delivery.send(snapshot);
        initializing = false;
        while (!pending.isEmpty()) {
            delivery.send(pending.remove());
        }
    }
}
