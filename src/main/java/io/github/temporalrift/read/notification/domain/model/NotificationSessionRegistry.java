package io.github.temporalrift.read.notification.domain.model;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NotificationSessionRegistry {

    private final Map<String, NotificationSession> sessions = new ConcurrentHashMap<>();

    public void register(NotificationSession session) {
        sessions.put(session.sessionId(), session);
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public Collection<NotificationSession> sessionsFor(UUID gameId) {
        return sessions.values().stream()
                .filter(session -> session.recipient().gameId().equals(gameId))
                .toList();
    }
}
