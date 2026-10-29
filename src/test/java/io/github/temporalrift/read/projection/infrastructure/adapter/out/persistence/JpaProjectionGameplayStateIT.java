package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.temporalrift.read.ReadServiceIntegrationTest;

/** A close of a simulation context discards the projections through this list, so it must name every table. */
@ReadServiceIntegrationTest
class JpaProjectionGameplayStateIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void everyTableOutsideTheClaimsAndTheExecutionsOwnBookkeepingIsAProjectionTable() {
        var tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                  AND table_name <> 'processed_events'
                  AND table_name NOT LIKE 'simulation\\_%' AND table_name NOT LIKE 'databasechangelog%'
                """, String.class);

        assertThat(tables).containsExactlyInAnyOrderElementsOf(JpaProjectionGameplayState.TABLES);
    }

    @Test
    void theListOrdersChildTablesBeforeTheTablesTheyReference() {
        var foreignKeys = jdbcTemplate.queryForList("""
                SELECT child.relname AS child, parent.relname AS parent
                FROM pg_constraint c
                JOIN pg_class child ON child.oid = c.conrelid
                JOIN pg_class parent ON parent.oid = c.confrelid
                WHERE c.contype = 'f'
                """);
        var order = JpaProjectionGameplayState.TABLES;

        assertThat(foreignKeys)
                .isNotEmpty()
                .allSatisfy(foreignKey -> assertThat(order.indexOf((String) foreignKey.get("child")))
                        .as("%s before %s", foreignKey.get("child"), foreignKey.get("parent"))
                        .isLessThan(order.indexOf((String) foreignKey.get("parent"))));
    }
}
