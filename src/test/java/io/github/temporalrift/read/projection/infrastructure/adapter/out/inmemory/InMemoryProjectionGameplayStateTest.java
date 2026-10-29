package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.github.temporalrift.read.ReadServiceApplication;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;

/** A close of a simulation context discards the projections through this list, so it must name every table. */
class InMemoryProjectionGameplayStateTest {

    @Test
    void everyTableOutsideTheClaimsAndTheExecutionsOwnBookkeepingIsAProjectionTable() {
        try (var host = new AnnotationConfigApplicationContext()) {
            host.refresh();
            try (var application = new SpringApplicationBuilder(ReadServiceApplication.class)
                    .parent(host)
                    .profiles("in-memory")
                    .run(
                            "--server.port=0",
                            "--game.rules.max-eras=5",
                            "--game.simulation.enabled=true",
                            "--spring.security.oauth2.resourceserver.jwt.issuer-uri=https://host-issuer.invalid",
                            "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://host-issuer.invalid/jwks")) {
                var tables = application.getBean(InMemoryDatabase.class).tableNames().stream()
                        .filter(table -> !table.equals("processed_events") && !table.startsWith("simulation_"))
                        .toList();

                assertThat(tables).containsExactlyInAnyOrderElementsOf(InMemoryProjectionGameplayState.TABLES);
            }
        }
    }
}
