package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;

@ExtendWith(MockitoExtension.class)
class JpaRevealedProbabilityIntelAdapterTest {

    @Mock
    RevealedProbabilityIntelJpaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private JpaRevealedProbabilityIntelAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaRevealedProbabilityIntelAdapter(repository, objectMapper);
    }

    @Test
    void upsertLatest_savesNewIntelWithItsCompleteIdentityAndOutcomes() {
        var intel = intel(2, 65);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        intel.gameId(), intel.playerId(), intel.eraNumber(), intel.eventId()))
                .willReturn(Optional.empty());

        adapter.upsertLatest(intel);

        var entityCaptor = ArgumentCaptor.forClass(RevealedProbabilityIntelEntity.class);
        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().toDomain(objectMapper)).isEqualTo(intel);
    }

    @Test
    void upsertLatest_replacesStoredOutcomesWhenTheNewObservationIsLater() {
        var original = intel(1, 35);
        var later = intel(2, 65);
        var entity = RevealedProbabilityIntelEntity.fromDomain(UUID.randomUUID(), original, objectMapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        later.gameId(), later.playerId(), later.eraNumber(), later.eventId()))
                .willReturn(Optional.of(entity));

        adapter.upsertLatest(later);

        assertThat(entity.toDomain(objectMapper)).isEqualTo(later);
    }

    @Test
    void upsertLatest_keepsStoredOutcomesWhenTheNewObservationIsOlder() {
        var latest = intel(2, 65);
        var older = intel(1, 35);
        var entity = RevealedProbabilityIntelEntity.fromDomain(UUID.randomUUID(), latest, objectMapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        older.gameId(), older.playerId(), older.eraNumber(), older.eventId()))
                .willReturn(Optional.of(entity));

        adapter.upsertLatest(older);

        assertThat(entity.toDomain(objectMapper)).isEqualTo(latest);
    }

    @Test
    void findByGameIdAndPlayerIdAndEraNumber_deserializesStoredOutcomes() {
        var intel = intel(2, 65);
        var entity = RevealedProbabilityIntelEntity.fromDomain(UUID.randomUUID(), intel, objectMapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(
                        intel.gameId(), intel.playerId(), intel.eraNumber()))
                .willReturn(List.of(entity));

        assertThat(adapter.findByGameIdAndPlayerIdAndEraNumber(intel.gameId(), intel.playerId(), intel.eraNumber()))
                .containsExactly(intel);
    }

    @Test
    void deleteByGameIdAndEraNumber_delegatesTheCurrentEraKey() {
        var gameId = UUID.randomUUID();

        adapter.deleteByGameIdAndEraNumber(gameId, 2);

        then(repository).should().deleteByGameIdAndEraNumber(gameId, 2);
    }

    private RevealedProbabilityIntel intel(int observedInRound, int probability) {
        return new RevealedProbabilityIntel(
                gameId,
                playerId,
                2,
                eventId,
                observedInRound,
                List.of(new RevealedProbabilityOutcome(UUID.randomUUID(), probability, false, false)));
    }
}
