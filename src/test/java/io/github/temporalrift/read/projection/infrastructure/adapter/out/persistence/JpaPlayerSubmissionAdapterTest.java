package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmissionChoice;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission.SubmissionWindow;

@ExtendWith(MockitoExtension.class)
class JpaPlayerSubmissionAdapterTest {

    @Mock
    PlayerSubmissionJpaRepository repository;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private JpaPlayerSubmissionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaPlayerSubmissionAdapter(repository);
    }

    @Test
    void upsert_savesNewActionSubmissionWithItsWindowRoundAndChoice() {
        var submission = new PlayerSubmission(gameId, playerId, 2, 1, SubmissionWindow.ACTION, SubmissionChoice.CARD);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndSubmissionWindowAndRoundNumber(
                        gameId, playerId, 2, "ACTION", 1))
                .willReturn(Optional.empty());

        adapter.upsert(submission);

        var entityCaptor = ArgumentCaptor.forClass(PlayerSubmissionEntity.class);
        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().toDomain()).isEqualTo(submission);
    }

    @Test
    void upsert_storesAWindowWithoutRoundOrChoiceUnderRoundZero() {
        var submission = new PlayerSubmission(gameId, playerId, 2, null, SubmissionWindow.DECLARATION, null);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndSubmissionWindowAndRoundNumber(
                        gameId, playerId, 2, "DECLARATION", 0))
                .willReturn(Optional.empty());

        adapter.upsert(submission);

        var entityCaptor = ArgumentCaptor.forClass(PlayerSubmissionEntity.class);
        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().toDomain()).isEqualTo(submission);
    }

    @Test
    void upsert_updatesTheExistingSlotInsteadOfAddingAnother() {
        var original = new PlayerSubmission(
                gameId, playerId, 2, null, SubmissionWindow.PARADOX_RESOLUTION, SubmissionChoice.PASS);
        var replacement = new PlayerSubmission(
                gameId, playerId, 2, null, SubmissionWindow.PARADOX_RESOLUTION, SubmissionChoice.CARD);
        var existing = PlayerSubmissionEntity.fromDomain(UUID.randomUUID(), original);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndSubmissionWindowAndRoundNumber(
                        gameId, playerId, 2, "PARADOX_RESOLUTION", 0))
                .willReturn(Optional.of(existing));

        adapter.upsert(replacement);

        then(repository).should(never()).save(ArgumentMatchers.any());
        assertThat(existing.toDomain()).isEqualTo(replacement);
    }

    @Test
    void findByGameIdAndPlayerIdAndEraNumber_readsStoredSubmissionsInWindowOrder() {
        var action = new PlayerSubmission(gameId, playerId, 2, 3, SubmissionWindow.ACTION, SubmissionChoice.SPECIAL);
        var handSelection = new PlayerSubmission(gameId, playerId, 2, null, SubmissionWindow.HAND_SELECTION, null);
        given(repository.findByGameIdAndPlayerIdAndEraNumberOrderBySubmissionWindowAscRoundNumberAsc(
                        gameId, playerId, 2))
                .willReturn(List.of(
                        PlayerSubmissionEntity.fromDomain(UUID.randomUUID(), action),
                        PlayerSubmissionEntity.fromDomain(UUID.randomUUID(), handSelection)));

        assertThat(adapter.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .containsExactly(action, handSelection);
    }

    @Test
    void deletes_delegateTheirKeys() {
        adapter.deleteByGameIdAndEraNumber(gameId, 2);
        adapter.deleteByGameId(gameId);

        then(repository).should().deleteByGameIdAndEraNumber(gameId, 2);
        then(repository).should().deleteByGameId(gameId);
        verifyNoMoreInteractions(repository);
    }
}
