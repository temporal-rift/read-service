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
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.model.ForesightPreviewEvent;
import io.github.temporalrift.read.projection.domain.model.ForesightPreviewOutcome;

@ExtendWith(MockitoExtension.class)
class JpaForesightPreviewAdapterTest {

    @Mock
    ForesightPreviewJpaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private JpaForesightPreviewAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaForesightPreviewAdapter(repository, objectMapper);
    }

    @Test
    void upsert_savesNewPreviewPreservingDeckOrder() {
        var preview = preview(List.of(
                new ForesightPreviewEvent(
                        UUID.randomUUID(),
                        "Collapse",
                        List.of(new ForesightPreviewOutcome(UUID.randomUUID(), "Falls"))),
                new ForesightPreviewEvent(UUID.randomUUID(), "Rise", List.of())));
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1))
                .willReturn(Optional.empty());

        adapter.upsert(preview);

        var entityCaptor = ArgumentCaptor.forClass(ForesightPreviewEntity.class);
        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().toDomain(objectMapper)).isEqualTo(preview);
    }

    @Test
    void upsert_overwritesTheStoredPreviewForTheSameViewerAndEra() {
        var stored = ForesightPreviewEntity.fromDomain(UUID.randomUUID(), preview(List.of()), objectMapper);
        var replacement = preview(List.of(new ForesightPreviewEvent(UUID.randomUUID(), "Rise", List.of())));
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1))
                .willReturn(Optional.of(stored));

        adapter.upsert(replacement);

        assertThat(stored.toDomain(objectMapper)).isEqualTo(replacement);
        then(repository).should(never()).save(any());
    }

    @Test
    void findByGameIdAndPlayerIdAndEraNumber_roundTripsTheEmptyReason() {
        var finalEra = new ForesightPreview(gameId, playerId, 3, 4, List.of(), "FINAL_ERA");
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 3))
                .willReturn(Optional.of(ForesightPreviewEntity.fromDomain(UUID.randomUUID(), finalEra, objectMapper)));

        assertThat(adapter.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 3))
                .contains(finalEra);
    }

    private ForesightPreview preview(List<ForesightPreviewEvent> events) {
        return new ForesightPreview(gameId, playerId, 1, 2, events, null);
    }
}
