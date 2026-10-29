package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;

/** Every table the projection's in-memory adapters define. */
@Repository
@Profile("in-memory")
class InMemoryProjectionGameplayState implements GameplayState {

    static final List<String> TABLES = List.of(
            "declaration_offer",
            "declaration_window",
            "detected_paradox",
            "expose_fact",
            "foresight_preview",
            "game_active_event",
            "game_chain",
            "game_history_projection",
            "game_player",
            "game_projection",
            "game_resolved_event",
            "game_terminal_final_score",
            "game_terminal_result",
            "game_terminal_winner",
            "player_game_state",
            "player_submission",
            "public_band",
            "public_declaration",
            "resolution_card_offer",
            "revealed_hand_card_intel",
            "revealed_influence_intel",
            "revealed_probability_intel");

    private final InMemoryDatabase database;

    InMemoryProjectionGameplayState(InMemoryDatabase database) {
        this.database = database;
    }

    @Override
    @Transactional
    public boolean isEmpty() {
        return database.isEmpty(TABLES);
    }

    @Override
    @Transactional
    public void discardAll() {
        database.clear(TABLES);
    }
}
