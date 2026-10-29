package io.github.temporalrift.read.simulation;

import static io.github.temporalrift.read.simulation.SimulationFacts.context;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.TerminalResultRepository;
import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.simulation.SimulationFacts.Fact;
import io.github.temporalrift.read.simulation.SimulationFacts.Game;

/**
 * The execution-control behaviour read-service promises on every profile: each case projects only its own game, and
 * a closed case leaves nothing a later case, or a replay of the same case, could read.
 */
public abstract class SimulationExecutionContract {

    static final String EXECUTION = "/internal/simulation/v1/execution";
    static final String CHECKPOINT = "/internal/simulation/v1/checkpoint";
    static final String CLOCK = "/internal/simulation/v1/clock";

    protected abstract MockMvc mvc();

    /** Delivers the record to every consumer group subscribed to its topic and returns once all handled it. */
    protected abstract void deliver(Fact fact);

    protected abstract <T> T bean(Class<T> type);

    protected abstract <T> Collection<T> beans(Class<T> type);

    /** Names of the tables outside the execution's own bookkeeping that still hold a row. */
    protected abstract Set<String> gameplayTablesHoldingRows();

    @AfterEach
    void closeTheOpenContext() throws Exception {
        var response =
                mvc().perform(get(CHECKPOINT).with(control())).andReturn().getResponse();
        if (response.getStatus() == 200) {
            String caseKey = JsonPath.read(response.getContentAsString(), "$.caseKey");
            close(UUID.fromString(caseKey)).andExpect(status().isNoContent());
        }
    }

    @Test
    void theCheckpointFollowsTheContextsGameFromReadyToTerminal() throws Exception {
        var caseKey = UUID.randomUUID();
        var game = new Game();

        configure(caseKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseKey").value(caseKey.toString()))
                .andExpect(jsonPath("$.rulesDigest").value(SimulationFacts.RULES_DIGEST))
                .andExpect(jsonPath("$.revision").value(0))
                .andExpect(jsonPath("$.logicalTime").value("2026-10-05T19:00:00Z"))
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.gameId").value(nullValue()))
                .andExpect(jsonPath("$.drained").value(true))
                .andExpect(jsonPath("$.dueTimersPending").value(0))
                .andExpect(jsonPath("$.nextDeadline").value(nullValue()));
        configure(caseKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("READY"));

        deliver(game.started);
        checkpoint()
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.gameId").value(game.gameId.toString()))
                .andExpect(jsonPath("$.sourceWatermarks").value(not(org.hamcrest.Matchers.empty())));

        deliver(game.ended);
        checkpoint()
                .andExpect(jsonPath("$.state").value("TERMINAL"))
                .andExpect(jsonPath("$.gameId").value(game.gameId.toString()))
                .andExpect(jsonPath("$.outboxPending").value(0))
                .andExpect(jsonPath("$.continuationsPending").value(0))
                .andExpect(jsonPath("$.dueTimersPending").value(0));
    }

    @Test
    void aReplayedCaseProjectsItsGameFromTheStart() throws Exception {
        var caseKey = UUID.randomUUID();
        var game = new Game();
        configure(caseKey).andExpect(status().isOk());
        deliver(game.started);
        deliver(game.scored);
        deliver(game.ended);
        assertThat(projection(game).phase()).isEqualTo(Phase.GAME_ENDED);

        close(caseKey).andExpect(status().isNoContent());
        configure(caseKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("READY"));
        // A replay derives the same records, event identities included.
        deliver(game.started);

        assertThat(projection(game).phase()).isEqualTo(Phase.LOBBY);
        assertThat(bean(GamePlayerRepository.class).findByGameId(game.gameId))
                .extracting(GamePlayer::score)
                .containsOnly(0);
        assertThat(bean(TerminalResultRepository.class).findByGameId(game.gameId))
                .isEmpty();
        checkpoint().andExpect(jsonPath("$.state").value("ACTIVE"));

        deliver(game.ended);

        assertThat(projection(game).phase()).isEqualTo(Phase.GAME_ENDED);
        assertThat(bean(TerminalResultRepository.class).findByGameId(game.gameId))
                .isPresent();
        checkpoint().andExpect(jsonPath("$.state").value("TERMINAL"));
    }

    @Test
    void aRecordDeliveredAfterItsContextClosedChangesNothing() throws Exception {
        var caseKey = UUID.randomUUID();
        var game = new Game();
        configure(caseKey).andExpect(status().isOk());
        deliver(game.started);
        close(caseKey).andExpect(status().isNoContent());

        deliver(game.ended);

        assertThat(bean(GameProjectionRepository.class).findByGameId(game.gameId))
                .isEmpty();
        assertThat(gameplayTablesHoldingRows()).isEmpty();
    }

    @Test
    void theGameOfAClosedContextNeverBindsTheNextContext() throws Exception {
        var closedCase = UUID.randomUUID();
        var closedGame = new Game();
        configure(closedCase).andExpect(status().isOk());
        deliver(closedGame.started);
        close(closedCase).andExpect(status().isNoContent());
        var nextCase = UUID.randomUUID();
        var nextGame = new Game();
        configure(nextCase).andExpect(status().isOk());

        deliver(closedGame.scored);

        checkpoint()
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.gameId").value(nullValue()));
        assertThat(bean(GameProjectionRepository.class).findByGameId(closedGame.gameId))
                .isEmpty();

        deliver(nextGame.started);
        deliver(closedGame.ended);

        checkpoint()
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.gameId").value(nextGame.gameId.toString()));
        assertThat(bean(GameProjectionRepository.class).findByGameId(closedGame.gameId))
                .isEmpty();
    }

    @Test
    void stateLeftByAnEarlierContextIsRefusedAsNotClean() throws Exception {
        var leftover = new Game();
        inTransaction(() -> bean(GameProjectionRepository.class).findByGameIdForUpdate(leftover.gameId));

        configure(UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_STATE_NOT_CLEAN"));
        checkpoint()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_NOT_CONFIGURED"));

        inTransaction(() -> beans(GameplayState.class).forEach(GameplayState::discardAll));
        inTransaction(() -> bean(ProcessedEventPort.class).claim(UUID.randomUUID(), "projection.game-events"));

        configure(UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_STATE_NOT_CLEAN"));

        inTransaction(() -> beans(GameplayState.class).forEach(GameplayState::discardAll));
        configure(UUID.randomUUID()).andExpect(status().isOk());
    }

    @Test
    void anotherContextWhileOneIsOpenIsAConflict() throws Exception {
        var open = UUID.randomUUID();
        configure(open).andExpect(status().isOk());

        configure(UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_CONTEXT_CONFLICT"));
        configure(open, "43")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_CONTEXT_CONFLICT"));
        checkpoint().andExpect(jsonPath("$.caseKey").value(open.toString()));
    }

    @Test
    void aCloseAnswersByTheCaseItNames() throws Exception {
        var first = UUID.randomUUID();
        configure(first).andExpect(status().isOk());

        close(first).andExpect(status().isNoContent());
        close(first).andExpect(status().isNoContent());
        close(UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_CONTEXT_CONFLICT"));

        var second = UUID.randomUUID();
        configure(second).andExpect(status().isOk());
        close(first).andExpect(status().isNoContent());
        checkpoint().andExpect(status().isOk()).andExpect(jsonPath("$.caseKey").value(second.toString()));
        close(UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_CONTEXT_CONFLICT"));
    }

    @Test
    void aMalformedContextIsRejected() throws Exception {
        var wrongDigest = context(UUID.randomUUID()).replace(SimulationFacts.RULES_DIGEST, "b".repeat(64));

        mvc().perform(put(EXECUTION)
                        .with(control())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongDigest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EXECUTION_CONTEXT"));
        mvc().perform(put(EXECUTION)
                        .with(control())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EXECUTION_CONTEXT"));
        checkpoint().andExpect(status().isConflict());
    }

    @Test
    void onlyASimulationControlTokenControlsTheService() throws Exception {
        mvc().perform(put(EXECUTION)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(context(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_FORBIDDEN"))
                .andExpect(jsonPath("$.caseKey").doesNotExist());
        mvc().perform(delete(EXECUTION)
                        .with(jwt())
                        .param("caseKey", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_FORBIDDEN"));
        mvc().perform(get(CHECKPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void advancingTheClockOnlyMovesLogicalTime() throws Exception {
        configure(UUID.randomUUID()).andExpect(status().isOk());
        var operation = UUID.randomUUID();

        advance(operation, 0, "2026-10-05T19:01:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedRevision").value(1))
                .andExpect(jsonPath("$.logicalTime").value("2026-10-05T19:01:00Z"));
        advance(operation, 0, "2026-10-05T19:01:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedRevision").value(1));
        advance(operation, 0, "2026-10-05T19:02:00Z")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        advance(UUID.randomUUID(), 0, "2026-10-05T19:02:00Z")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_EXECUTION_REVISION"));
        advance(UUID.randomUUID(), 1, "2026-10-05T19:00:30Z")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CLOCK_REGRESSION"));

        checkpoint()
                .andExpect(jsonPath("$.revision").value(1))
                .andExpect(jsonPath("$.logicalTime").value("2026-10-05T19:01:00Z"))
                .andExpect(jsonPath("$.dueTimersPending").value(0))
                .andExpect(jsonPath("$.nextDeadline").value(nullValue()));
    }

    @Test
    void aCloseLeavesNoGameplayRowBehind() throws Exception {
        var caseKey = UUID.randomUUID();
        var game = new Game();
        configure(caseKey).andExpect(status().isOk());
        deliver(game.started);
        game.midGame().forEach(this::deliver);
        deliver(game.ended);
        assertThat(gameplayTablesHoldingRows()).isNotEmpty();

        close(caseKey).andExpect(status().isNoContent());

        assertThat(gameplayTablesHoldingRows()).isEmpty();
    }

    protected ResultActions configure(UUID caseKey) throws Exception {
        return configure(caseKey, "42");
    }

    protected ResultActions configure(UUID caseKey, String seed) throws Exception {
        return mvc().perform(put(EXECUTION)
                .with(control())
                .contentType(MediaType.APPLICATION_JSON)
                .content(context(caseKey, seed)));
    }

    protected ResultActions close(UUID caseKey) throws Exception {
        return mvc().perform(delete(EXECUTION).with(control()).param("caseKey", caseKey.toString()));
    }

    protected ResultActions checkpoint() throws Exception {
        return mvc().perform(get(CHECKPOINT).with(control()));
    }

    protected ResultActions advance(UUID operationId, long expectedRevision, String targetTime) throws Exception {
        return mvc().perform(put(CLOCK)
                .with(control())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"operationId":"%s","expectedRevision":%d,"targetTime":"%s"}
                        """.formatted(operationId, expectedRevision, targetTime)));
    }

    protected static RequestPostProcessor control() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_simulation:control"));
    }

    protected void inTransaction(Runnable work) {
        bean(TransactionTemplate.class).executeWithoutResult(status -> work.run());
    }

    private io.github.temporalrift.read.projection.domain.model.GameProjection projection(Game game) {
        return bean(GameProjectionRepository.class).findByGameId(game.gameId).orElseThrow();
    }
}
