package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Map;
import java.util.UUID;

public interface PlayerNameRepository {

    /** Latest join wins; re-applying the same name is a no-op. */
    void upsert(UUID gameId, UUID playerId, String playerName);

    Map<UUID, String> findByGameId(UUID gameId);
}
