package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.RoundActionSummary;

@ExtendWith(MockitoExtension.class)
class JpaGameProjectionAdapterTest {

    @Mock
    GameProjectionJpaRepository repository;

    private final UUID gameId = UUID.randomUUID();
    private JpaGameProjectionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaGameProjectionAdapter(repository);
    }

    @Test
    void saveNew_retainsThresholdPhaseAffectedEventsAndSummary() {
        var affected = UUID.randomUUID();
        var paradox = UUID.randomUUID();
        var summary = new LastRoundSummary(
                2, 3, List.of(new RoundActionSummary(UUID.randomUUID(), "INFORMATION", "CARD", false)));
        var projection = new GameProjection(
                gameId,
                2,
                Phase.PARADOX_RESOLUTION,
                List.of(paradox),
                summary,
                3,
                null,
                Instant.parse("2026-09-30T12:01:00Z"),
                9,
                Instant.EPOCH,
                List.of(affected),
                20);
        given(repository.findById(gameId)).willReturn(Optional.empty());

        adapter.save(projection);

        var captor = ArgumentCaptor.forClass(GameProjectionEntity.class);
        then(repository).should().save(captor.capture());
        var stored = captor.getValue().toDomain();
        assertThat(stored.phase()).isEqualTo(Phase.PARADOX_RESOLUTION);
        assertThat(stored.pendingParadoxIds()).containsExactly(paradox);
        assertThat(stored.affectedEventIds()).containsExactly(affected);
        assertThat(stored.lastRoundSummary()).isEqualTo(summary);
        assertThat(stored.winScoreThreshold()).isEqualTo(20);
        assertThat(stored.revision()).isZero();
        assertThat(stored.lastUpdatedAt()).isAfter(Instant.EPOCH);
    }

    @Test
    void saveExisting_replacesAffectedEventsAndAdvancesRevision() {
        var oldEvent = UUID.randomUUID();
        var newEvent = UUID.randomUUID();
        var existing = GameProjectionEntity.fromDomain(new GameProjection(
                gameId, 2, Phase.ACTION_ROUND_3, List.of(), null, 3, null, null, 4, Instant.EPOCH, List.of(oldEvent)));
        given(repository.findById(gameId)).willReturn(Optional.of(existing));

        adapter.save(new GameProjection(
                gameId,
                2,
                Phase.PARADOX_RESOLUTION,
                List.of(UUID.randomUUID()),
                null,
                3,
                null,
                Instant.EPOCH.plusSeconds(30),
                4,
                Instant.EPOCH,
                List.of(newEvent)));

        assertThat(existing.toDomain().affectedEventIds()).containsExactly(newEvent);
        assertThat(existing.toDomain().revision()).isEqualTo(5);
        assertThat(existing.toDomain().lastRoundSummary()).isNull();
        then(repository).should(never()).save(existing);
    }

    @Test
    void saveExisting_preservesTheThresholdWhenAdvancingTheProjection() {
        var existing = GameProjectionEntity.fromDomain(
                new GameProjection(gameId, 2, Phase.ACTION_ROUND_3).withWinScoreThreshold(20));
        given(repository.findById(gameId)).willReturn(Optional.of(existing));

        adapter.save(new GameProjection(gameId, 2, Phase.PARADOX_RESOLUTION));

        assertThat(existing.toDomain().winScoreThreshold()).isEqualTo(20);
    }

    @Test
    void findForUpdate_insertsAnchorThenReturnsLockedProjection() {
        var projection = new GameProjection(gameId, 1, Phase.ERA_START);
        given(repository.lockByGameId(gameId)).willReturn(Optional.of(GameProjectionEntity.fromDomain(projection)));

        assertThat(adapter.findByGameIdForUpdate(gameId)).contains(projection);
        then(repository).should().insertAnchorIfAbsent(gameId);
    }

    @Test
    void findByGameId_returnsPersistedProjection() {
        var projection = new GameProjection(gameId, 1, Phase.ERA_START);
        given(repository.findById(gameId)).willReturn(Optional.of(GameProjectionEntity.fromDomain(projection)));

        assertThat(adapter.findByGameId(gameId)).contains(projection);
    }
}
