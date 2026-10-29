package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.shared.GameplayState;

/** Every projection table; child tables are emptied before the tables their foreign keys reference. */
@Repository
@Profile("!in-memory")
class JpaProjectionGameplayState implements GameplayState {

    static final List<String> TABLES = List.of(
            "game_projection_affected_event",
            "game_projection_last_round_summary_action",
            "game_projection_pending_paradox",
            "game_active_event_outcome",
            "player_game_state_hand_card",
            "player_game_state_pending_hand_card",
            "declaration_offer",
            "declaration_window",
            "foresight_preview",
            "game_active_event",
            "game_chain_projection",
            "game_declaration",
            "game_detected_paradox",
            "game_expose_fact",
            "game_history_projection",
            "game_player",
            "game_projection",
            "game_public_band",
            "game_resolved_event",
            "game_terminal_final_score",
            "game_terminal_result",
            "game_terminal_winner",
            "player_game_state",
            "player_submission",
            "resolution_card_offer",
            "revealed_hand_card_intel",
            "revealed_influence_intel",
            "revealed_probability_intel");

    // Child tables hold no row without their parent, so the parents are enough to tell whether anything is held.
    private static final String ANY_ROW = """
            SELECT EXISTS (SELECT 1 FROM declaration_offer)
                OR EXISTS (SELECT 1 FROM declaration_window)
                OR EXISTS (SELECT 1 FROM foresight_preview)
                OR EXISTS (SELECT 1 FROM game_active_event)
                OR EXISTS (SELECT 1 FROM game_chain_projection)
                OR EXISTS (SELECT 1 FROM game_declaration)
                OR EXISTS (SELECT 1 FROM game_detected_paradox)
                OR EXISTS (SELECT 1 FROM game_expose_fact)
                OR EXISTS (SELECT 1 FROM game_history_projection)
                OR EXISTS (SELECT 1 FROM game_player)
                OR EXISTS (SELECT 1 FROM game_projection)
                OR EXISTS (SELECT 1 FROM game_public_band)
                OR EXISTS (SELECT 1 FROM game_resolved_event)
                OR EXISTS (SELECT 1 FROM game_terminal_final_score)
                OR EXISTS (SELECT 1 FROM game_terminal_result)
                OR EXISTS (SELECT 1 FROM game_terminal_winner)
                OR EXISTS (SELECT 1 FROM player_game_state)
                OR EXISTS (SELECT 1 FROM player_submission)
                OR EXISTS (SELECT 1 FROM resolution_card_offer)
                OR EXISTS (SELECT 1 FROM revealed_hand_card_intel)
                OR EXISTS (SELECT 1 FROM revealed_influence_intel)
                OR EXISTS (SELECT 1 FROM revealed_probability_intel)
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean isEmpty() {
        return !Boolean.TRUE.equals(
                entityManager.createNativeQuery(ANY_ROW, Boolean.class).getSingleResult());
    }

    @Override
    @Transactional
    public void discardAll() {
        // A managed entity of a discarded row must never be flushed back afterwards.
        entityManager.flush();
        entityManager.clear();
        TABLES.forEach(table -> entityManager
                .createNativeQuery("DELETE FROM " + table) // NOSONAR a fixed table name, never input
                .executeUpdate());
    }
}
