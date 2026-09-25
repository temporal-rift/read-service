package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;

@ExtendWith(MockitoExtension.class)
class JpaGamePlayerAdapterTest {

    @Mock
    GamePlayerJpaRepository repository;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private JpaGamePlayerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaGamePlayerAdapter(repository);
    }

    @Test
    void save_storesANewPlayerWithEveryField() {
        var player = new GamePlayer(playerId, 4, true, "ERASERS", "Ada");
        given(repository.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        adapter.save(gameId, player);

        var captor = ArgumentCaptor.forClass(GamePlayerEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().getGameId()).isEqualTo(gameId);
        assertThat(captor.getValue().toDomain()).isEqualTo(player);
    }

    @Test
    void save_updatesAnExistingRowInPlace() {
        var entity = GamePlayerEntity.fromDomain(UUID.randomUUID(), gameId, new GamePlayer(playerId, 0, true, null));
        given(repository.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(entity));
        var updated = new GamePlayer(playerId, 9, false, "PROPHETS", "Ada");

        adapter.save(gameId, updated);

        assertThat(entity.toDomain()).isEqualTo(updated);
        then(repository).should(never()).save(any());
    }

    @Test
    void findByGameId_returnsEveryPlayerWithTheirName() {
        var entity =
                GamePlayerEntity.fromDomain(UUID.randomUUID(), gameId, new GamePlayer(playerId, 2, true, null, "Ben"));
        given(repository.findByGameId(gameId)).willReturn(List.of(entity));

        assertThat(adapter.findByGameId(gameId)).containsExactly(new GamePlayer(playerId, 2, true, null, "Ben"));
    }

    @Test
    void findByGameIdAndPlayerId_mapsTheStoredRow() {
        var entity =
                GamePlayerEntity.fromDomain(UUID.randomUUID(), gameId, new GamePlayer(playerId, 1, false, null, "Cy"));
        given(repository.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(entity));

        assertThat(adapter.findByGameIdAndPlayerId(gameId, playerId))
                .contains(new GamePlayer(playerId, 1, false, null, "Cy"));
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getPlayerId()).isEqualTo(playerId);
    }
}
