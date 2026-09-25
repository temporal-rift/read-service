package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaPlayerNameAdapterTest {

    @Mock
    PlayerNameJpaRepository repository;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private JpaPlayerNameAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaPlayerNameAdapter(repository);
    }

    @Test
    void upsert_savesANewNameForAnUnseenPlayer() {
        given(repository.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        adapter.upsert(gameId, playerId, "Ada");

        var captor = ArgumentCaptor.forClass(PlayerNameEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().playerId()).isEqualTo(playerId);
        assertThat(captor.getValue().playerName()).isEqualTo("Ada");
    }

    @Test
    void upsert_overwritesTheStoredNameOnRejoin() {
        var existing = new PlayerNameEntity(UUID.randomUUID(), gameId, playerId, "Dee");
        given(repository.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(existing));

        adapter.upsert(gameId, playerId, "Dana");

        assertThat(existing.playerName()).isEqualTo("Dana");
        then(repository).should(never()).save(any());
    }

    @Test
    void findByGameId_mapsEachPlayerToTheirName() {
        var other = UUID.randomUUID();
        given(repository.findByGameId(gameId))
                .willReturn(List.of(
                        new PlayerNameEntity(UUID.randomUUID(), gameId, playerId, "Ada"),
                        new PlayerNameEntity(UUID.randomUUID(), gameId, other, "Ben")));

        assertThat(adapter.findByGameId(gameId)).isEqualTo(Map.of(playerId, "Ada", other, "Ben"));
    }
}
