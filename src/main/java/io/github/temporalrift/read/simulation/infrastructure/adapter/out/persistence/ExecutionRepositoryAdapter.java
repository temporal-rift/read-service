package io.github.temporalrift.read.simulation.infrastructure.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.simulation.domain.execution.EntropyVersion;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionScope;
import io.github.temporalrift.read.simulation.domain.execution.Seed;
import io.github.temporalrift.read.simulation.domain.execution.SimulationSeat;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@Component
@Profile("!in-memory")
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class ExecutionRepositoryAdapter implements ExecutionRepository {

    private static final String SELECT = """
            SELECT case_key, seed, entropy_version, manifest_digest, rules_digest, configured_logical_time,
                   logical_time, revision, game_id
            FROM simulation_execution WHERE id = 1
            """;

    private static final String SELECT_SCOPE = "SELECT case_key, game_id FROM simulation_execution WHERE id = 1";

    private static final String CASE_KEY = "caseKey";
    private static final String GAME_ID = "gameId";
    private static final String REVISION = "revision";

    private final JdbcClient jdbc;

    ExecutionRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Execution> find() {
        return jdbc.sql(SELECT).query(this::toExecution).optional();
    }

    @Override
    public Optional<Execution> findWithLock() {
        return jdbc.sql(SELECT + " FOR UPDATE").query(this::toExecution).optional();
    }

    @Override
    public boolean insertIfAbsent(Execution execution) {
        var context = execution.context();
        int inserted = jdbc.sql("""
                        INSERT INTO simulation_execution
                            (id, case_key, seed, entropy_version, manifest_digest, rules_digest,
                             configured_logical_time, logical_time, revision)
                        VALUES (1, :caseKey, :seed, :entropyVersion, :manifestDigest, :rulesDigest,
                                :configuredLogicalTime, :logicalTime, :revision)
                        ON CONFLICT (id) DO NOTHING
                        """)
                .param(CASE_KEY, context.caseKey())
                .param("seed", context.seed().decimal())
                .param("entropyVersion", context.entropyVersion().name())
                .param("manifestDigest", context.manifestDigest())
                .param("rulesDigest", context.rulesDigest())
                .param("configuredLogicalTime", utc(context.logicalTime()))
                .param("logicalTime", utc(execution.logicalTime()))
                .param(REVISION, execution.revision())
                .update();
        if (inserted == 0) {
            return false;
        }
        context.seats()
                .forEach(seat -> jdbc.sql("""
                                INSERT INTO simulation_seat (seat_index, player_id, faction)
                                VALUES (:seatIndex, :playerId, :faction)
                                """)
                        .param("seatIndex", seat.seatIndex())
                        .param("playerId", seat.playerId())
                        .param("faction", seat.faction())
                        .update());
        return true;
    }

    @Override
    public void update(Execution execution) {
        jdbc.sql("UPDATE simulation_execution SET logical_time = :logicalTime, revision = :revision WHERE id = 1")
                .param("logicalTime", utc(execution.logicalTime()))
                .param(REVISION, execution.revision())
                .update();
    }

    @Override
    public Optional<ExecutionScope> findScope() {
        return jdbc.sql(SELECT_SCOPE).query(this::toScope).optional();
    }

    @Override
    public Optional<ExecutionScope> findScopeForHandling() {
        return jdbc.sql(SELECT_SCOPE + " FOR SHARE").query(this::toScope).optional();
    }

    @Override
    public Optional<ExecutionScope> findScopeForBinding() {
        return jdbc.sql(SELECT_SCOPE + " FOR UPDATE").query(this::toScope).optional();
    }

    @Override
    public void bindGame(UUID caseKey, UUID gameId) {
        jdbc.sql("""
                        UPDATE simulation_execution SET game_id = :gameId
                        WHERE id = 1 AND case_key = :caseKey AND game_id IS NULL
                        """).param(GAME_ID, gameId).param(CASE_KEY, caseKey).update();
    }

    @Override
    public void close(Execution execution) {
        var caseKey = execution.context().caseKey();
        jdbc.sql("DELETE FROM simulation_seat").update();
        jdbc.sql("DELETE FROM simulation_execution WHERE id = 1").update();
        jdbc.sql("""
                        INSERT INTO simulation_closed_execution (id, case_key) VALUES (1, :caseKey)
                        ON CONFLICT (id) DO UPDATE SET case_key = EXCLUDED.case_key
                        """).param(CASE_KEY, caseKey).update();
        if (execution.gameId() != null) {
            jdbc.sql("""
                            INSERT INTO simulation_closed_game (game_id, case_key) VALUES (:gameId, :caseKey)
                            ON CONFLICT (game_id) DO UPDATE SET case_key = EXCLUDED.case_key
                            """)
                    .param(GAME_ID, execution.gameId())
                    .param(CASE_KEY, caseKey)
                    .update();
        }
    }

    @Override
    public Optional<UUID> lastClosedCaseKey() {
        return jdbc.sql("SELECT case_key FROM simulation_closed_execution WHERE id = 1")
                .query(UUID.class)
                .optional();
    }

    @Override
    public boolean isClosedGame(UUID gameId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM simulation_closed_game WHERE game_id = :gameId)")
                .param(GAME_ID, gameId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public void forgetClosedGames(UUID caseKey) {
        jdbc.sql("DELETE FROM simulation_closed_game WHERE case_key = :caseKey")
                .param(CASE_KEY, caseKey)
                .update();
    }

    private Execution toExecution(ResultSet rs, int row) throws SQLException {
        var context = new ExecutionContext(
                rs.getObject("case_key", UUID.class),
                new Seed(rs.getString("seed")),
                EntropyVersion.valueOf(rs.getString("entropy_version")),
                rs.getString("manifest_digest"),
                rs.getString("rules_digest"),
                instant(rs, "configured_logical_time"),
                seats());
        return new Execution(
                context, rs.getLong(REVISION), instant(rs, "logical_time"), rs.getObject("game_id", UUID.class));
    }

    private ExecutionScope toScope(ResultSet rs, int row) throws SQLException {
        return new ExecutionScope(rs.getObject("case_key", UUID.class), rs.getObject("game_id", UUID.class));
    }

    private List<SimulationSeat> seats() {
        return jdbc.sql("SELECT seat_index, player_id, faction FROM simulation_seat ORDER BY seat_index")
                .query((rs, row) -> new SimulationSeat(
                        rs.getInt("seat_index"), rs.getObject("player_id", UUID.class), rs.getString("faction")))
                .list();
    }

    static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }
}
