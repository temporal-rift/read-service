package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.DetectedParadox;
import io.github.temporalrift.read.projection.domain.model.ParadoxType;

@ExtendWith(MockitoExtension.class)
class JpaDetectedParadoxAdapterTest {

    @Mock
    DetectedParadoxJpaRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID gameId = UUID.randomUUID();
    private JpaDetectedParadoxAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaDetectedParadoxAdapter(repository, mapper);
    }

    @Test
    void saveIfAbsentAndRead_preservesTheParadoxDetail() {
        var paradox = new DetectedParadox(
                gameId,
                2,
                UUID.randomUUID(),
                ParadoxType.IMPOSSIBLE_ERASURE,
                UUID.randomUUID(),
                List.of(UUID.randomUUID(), UUID.randomUUID()));
        given(repository.existsById(paradox.paradoxId())).willReturn(false);

        adapter.saveIfAbsent(paradox);

        var captor = ArgumentCaptor.forClass(DetectedParadoxEntity.class);
        then(repository).should().save(captor.capture());
        given(repository.findByGameIdAndParadoxIdIn(gameId, List.of(paradox.paradoxId())))
                .willReturn(List.of(captor.getValue()));
        assertThat(adapter.findByGameIdAndParadoxIds(gameId, List.of(paradox.paradoxId())))
                .containsExactly(paradox);
    }

    @Test
    void saveIfAbsent_redeliveredParadox_keepsTheRecordedDetail() {
        var paradox = new DetectedParadox(
                gameId, 2, UUID.randomUUID(), ParadoxType.DEAD_HEAT, UUID.randomUUID(), List.of(UUID.randomUUID()));
        given(repository.existsById(paradox.paradoxId())).willReturn(true);

        adapter.saveIfAbsent(paradox);

        then(repository).should(never()).save(any());
    }

    @Test
    void findWithoutIds_returnsNothingWithoutQuerying() {
        assertThat(adapter.findByGameIdAndParadoxIds(gameId, List.of())).isEmpty();

        then(repository).shouldHaveNoInteractions();
    }

    @Test
    void deleteOperations_removeOnlyTheRequestedGameScope() {
        adapter.deleteByGameIdAndEraNumber(gameId, 2);
        adapter.deleteByGameId(gameId);

        then(repository).should().deleteByGameIdAndEraNumber(gameId, 2);
        then(repository).should().deleteByGameId(gameId);
    }
}
