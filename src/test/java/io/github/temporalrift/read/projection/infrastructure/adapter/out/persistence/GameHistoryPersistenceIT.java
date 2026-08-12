package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.TestSecurityConfig;
import io.github.temporalrift.read.TestcontainersConfiguration;
import io.github.temporalrift.read.projection.domain.model.CarryForwardProbability;
import io.github.temporalrift.read.projection.domain.model.DealtCard;
import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.model.HistoryEventDefinition;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class GameHistoryPersistenceIT {

    @Autowired
    GameHistoryRepository histories;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void saveLoad_preservesCorrelationMetadataAndCascadeClosure() {
        var gameId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var carryForwardProbability = new CarryForwardProbability(outcomeId, 45);
        var history = history(gameId, 1, eventId, outcomeId)
                .recordResolvedOutcome(eventId, outcomeId)
                .recordCascade(eventId, List.of(carryForwardProbability))
                .close(1);

        save(history);

        var restored = histories.findByGameIdAndEraNumber(gameId, 1).orElseThrow();
        assertThat(restored.resolvedOutcomes().getFirst().winningOutcomeDescription())
                .isEqualTo("Outcome 1");
        assertThat(restored.cascadedEvents().getFirst().title()).isEqualTo("Event 1");
        assertThat(restored.cascadedEventReferences().getFirst().carryForwardProbabilityState())
                .containsExactly(carryForwardProbability);
        assertThat(restored.paradoxesCascaded()).isEqualTo(1);
        assertThat(restored.closed()).isTrue();
    }

    @Test
    void saveLoad_preservesDealtHandsPerPlayer() {
        var gameId = UUID.randomUUID();
        var firstPlayerId = UUID.randomUUID();
        var secondPlayerId = UUID.randomUUID();
        var firstCard = new DealtCard(UUID.randomUUID(), "PUSH", "I");
        var secondCard = new DealtCard(UUID.randomUUID(), "SCAN", "II");
        var history = history(gameId, 1, UUID.randomUUID(), UUID.randomUUID())
                .recordDealtHand(firstPlayerId, List.of(firstCard))
                .recordDealtHand(secondPlayerId, List.of(secondCard));

        save(history);

        var restored = histories.findByGameIdAndEraNumber(gameId, 1).orElseThrow();
        assertThat(restored.myHand(firstPlayerId)).containsExactly(firstCard);
        assertThat(restored.myHand(secondPlayerId)).containsExactly(secondCard);
    }

    @Test
    void save_ordersMultipleErasAndKeepsOneRowPerIdentity() {
        var gameId = UUID.randomUUID();
        save(history(gameId, 2, UUID.randomUUID(), UUID.randomUUID()));
        var eraOne = history(gameId, 1, UUID.randomUUID(), UUID.randomUUID());
        save(eraOne);
        save(eraOne.close(0));

        assertThat(histories.findByGameId(gameId))
                .extracting(GameHistoryProjection::eraNumber)
                .containsExactly(1, 2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM game_history_projection WHERE game_id = ? AND era_number = 1",
                        Integer.class,
                        gameId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT version FROM game_history_projection WHERE game_id = ? AND era_number = 1",
                        Long.class,
                        gameId))
                .isPositive();
    }

    @Test
    void concurrentUpdates_raiseOptimisticConflict() {
        var gameId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        save(history(gameId, 1, eventId, outcomeId));

        var firstManager = entityManagerFactory.createEntityManager();
        var secondManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();
            var first = load(firstManager, gameId);
            var second = load(secondManager, gameId);
            first.updateFrom(first.toDomain(objectMapper).recordResolvedOutcome(eventId, outcomeId), objectMapper);
            second.updateFrom(second.toDomain(objectMapper).recordCascade(eventId, List.of()), objectMapper);

            firstManager.getTransaction().commit();

            assertThatThrownBy(() -> secondManager.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(jakarta.persistence.OptimisticLockException.class);
        } finally {
            if (firstManager.getTransaction().isActive()) {
                firstManager.getTransaction().rollback();
            }
            if (secondManager.getTransaction().isActive()) {
                secondManager.getTransaction().rollback();
            }
            firstManager.close();
            secondManager.close();
        }
    }

    private GameHistoryProjectionEntity load(jakarta.persistence.EntityManager manager, UUID gameId) {
        return manager.createQuery(
                        "select history from GameHistoryProjectionEntity history "
                                + "where history.gameId = :gameId and history.eraNumber = 1",
                        GameHistoryProjectionEntity.class)
                .setParameter("gameId", gameId)
                .getSingleResult();
    }

    private void save(GameHistoryProjection history) {
        transactions.executeWithoutResult(status -> histories.save(history));
    }

    private GameHistoryProjection history(UUID gameId, int eraNumber, UUID eventId, UUID outcomeId) {
        return GameHistoryProjection.empty(gameId, eraNumber)
                .mergeEventDefinitions(List.of(new HistoryEventDefinition(
                        eventId,
                        0,
                        "Event " + eraNumber,
                        List.of(new EventOutcome(outcomeId, "Outcome " + eraNumber)))));
    }
}
