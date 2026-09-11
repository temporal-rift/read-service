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

import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;

@ExtendWith(MockitoExtension.class)
class JpaRevealedInfluenceIntelAdapterTest {

    @Mock
    RevealedInfluenceIntelJpaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private JpaRevealedInfluenceIntelAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaRevealedInfluenceIntelAdapter(repository, objectMapper);
    }

    @Test
    void upsertLatest_savesNewIntelWithItsCompleteIdentityAndInfluencers() {
        var intel = intel(2, List.of(UUID.randomUUID(), UUID.randomUUID()));
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        intel.gameId(), intel.playerId(), intel.eraNumber(), intel.eventId()))
                .willReturn(Optional.empty());

        adapter.upsertLatest(intel);

        var entityCaptor = ArgumentCaptor.forClass(RevealedInfluenceIntelEntity.class);
        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().toDomain(objectMapper)).isEqualTo(intel);
    }

    @Test
    void upsertLatest_savesEmptyInfluencerSetRatherThanDroppingIt() {
        var intel = intel(1, List.of());
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        intel.gameId(), intel.playerId(), intel.eraNumber(), intel.eventId()))
                .willReturn(Optional.empty());

        adapter.upsertLatest(intel);

        var entityCaptor = ArgumentCaptor.forClass(RevealedInfluenceIntelEntity.class);
        then(repository).should().save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().toDomain(objectMapper)).isEqualTo(intel);
    }

    @Test
    void upsertLatest_replacesStoredInfluencersWhenTheNewObservationIsLater() {
        var original = intel(1, List.of(UUID.randomUUID()));
        var later = intel(2, List.of(UUID.randomUUID(), UUID.randomUUID()));
        var entity = RevealedInfluenceIntelEntity.fromDomain(UUID.randomUUID(), original, objectMapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        later.gameId(), later.playerId(), later.eraNumber(), later.eventId()))
                .willReturn(Optional.of(entity));

        adapter.upsertLatest(later);

        assertThat(entity.toDomain(objectMapper)).isEqualTo(later);
    }

    @Test
    void upsertLatest_keepsStoredInfluencersWhenTheNewObservationIsOlder() {
        var latest = intel(2, List.of(UUID.randomUUID()));
        var older = intel(1, List.of());
        var entity = RevealedInfluenceIntelEntity.fromDomain(UUID.randomUUID(), latest, objectMapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumberAndEventId(
                        older.gameId(), older.playerId(), older.eraNumber(), older.eventId()))
                .willReturn(Optional.of(entity));

        adapter.upsertLatest(older);

        assertThat(entity.toDomain(objectMapper)).isEqualTo(latest);
    }

    @Test
    void findByGameIdAndPlayerIdAndEraNumber_deserializesStoredInfluencers() {
        var intel = intel(2, List.of(UUID.randomUUID()));
        var entity = RevealedInfluenceIntelEntity.fromDomain(UUID.randomUUID(), intel, objectMapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumberOrderByEventIdAsc(
                        intel.gameId(), intel.playerId(), intel.eraNumber()))
                .willReturn(List.of(entity));

        assertThat(adapter.findByGameIdAndPlayerIdAndEraNumber(intel.gameId(), intel.playerId(), intel.eraNumber()))
                .containsExactly(intel);
    }

    @Test
    void deleteByGameIdAndEraNumber_delegatesTheCurrentEraKey() {
        adapter.deleteByGameIdAndEraNumber(gameId, 2);

        then(repository).should().deleteByGameIdAndEraNumber(gameId, 2);
    }

    @Test
    void deleteByGameId_delegatesTheWholeGameKey() {
        adapter.deleteByGameId(gameId);

        then(repository).should().deleteByGameId(gameId);
    }

    private RevealedInfluenceIntel intel(int observedInRound, List<UUID> influencers) {
        return new RevealedInfluenceIntel(gameId, playerId, 2, eventId, observedInRound, influencers);
    }
}
