package io.github.temporalrift.read.simulation;

import static io.github.temporalrift.read.simulation.SimulationFacts.context;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.ReadServiceApplication;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.simulation.SimulationFacts.Fact;
import io.github.temporalrift.read.simulation.SimulationFacts.Game;

/** The execution controls of the {@code in-memory} profile, started the way a simulator slot hosts read-service. */
class InMemorySimulationExecutionTest extends SimulationExecutionContract {

    private static final List<ConfigurableApplicationContext> started = new ArrayList<>();
    private static Service service;

    @BeforeAll
    static void startService() {
        service = Service.start(true);
    }

    @AfterAll
    static void stopServices() {
        started.reversed().forEach(ConfigurableApplicationContext::close);
        started.clear();
    }

    @Override
    protected MockMvc mvc() {
        return service.mvc();
    }

    @Override
    protected void deliver(Fact fact) {
        service.deliver(fact);
    }

    @Override
    protected <T> T bean(Class<T> type) {
        return service.application().getBean(type);
    }

    @Override
    protected <T> Collection<T> beans(Class<T> type) {
        return service.application().getBeansOfType(type).values();
    }

    @Override
    protected Set<String> gameplayTablesHoldingRows() {
        var database = bean(InMemoryDatabase.class);
        return database.tableNames().stream()
                .filter(table -> !table.startsWith("simulation_"))
                .filter(table -> !database.isEmpty(List.of(table)))
                .collect(Collectors.toSet());
    }

    @Test
    void aServiceThatWasNeverConfiguredSaysSo() throws Exception {
        var fresh = Service.start(true);

        fresh.mvc()
                .perform(get(CHECKPOINT).with(control()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_NOT_CONFIGURED"));
        fresh.mvc()
                .perform(delete(EXECUTION)
                        .with(control())
                        .param("caseKey", UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_NOT_CONFIGURED"));
    }

    @Test
    void anOrdinaryDeploymentHasNoExecutionControls() throws Exception {
        var ordinary = Service.start(false);

        ordinary.mvc()
                .perform(delete(EXECUTION)
                        .with(control())
                        .param("caseKey", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_UNAVAILABLE"));
        ordinary.mvc()
                .perform(get(CHECKPOINT).with(control()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_UNAVAILABLE"));
    }

    @Test
    void closingOneInstancesContextLeavesAnotherInstanceUnchanged() throws Exception {
        var first = Service.start(true);
        var second = Service.start(true);
        var caseKey = UUID.randomUUID();
        var game = new Game();
        for (var instance : List.of(first, second)) {
            instance.configure(caseKey);
            instance.deliver(game.started);
        }

        first.mvc()
                .perform(delete(EXECUTION).with(control()).param("caseKey", caseKey.toString()))
                .andExpect(status().isNoContent());

        assertThat(first.application().getBean(GameProjectionRepository.class).findByGameId(game.gameId))
                .isEmpty();
        assertThat(second.application().getBean(GameProjectionRepository.class).findByGameId(game.gameId))
                .isPresent();
        second.mvc()
                .perform(get(CHECKPOINT).with(control()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.gameId").value(game.gameId.toString()));
    }

    private record Service(ConfigurableApplicationContext application, MockMvc mvc) {

        static Service start(boolean simulation) {
            var host = new AnnotationConfigApplicationContext();
            host.refresh();
            started.add(host);
            var application = new SpringApplicationBuilder(ReadServiceApplication.class)
                    .parent(host)
                    .profiles("in-memory")
                    .run(
                            "--server.port=0",
                            "--game.rules.max-eras=5",
                            "--game.simulation.enabled=" + simulation,
                            "--spring.security.oauth2.resourceserver.jwt.issuer-uri=https://host-issuer.invalid",
                            "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://host-issuer.invalid/jwks");
            started.add(application);
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) application)
                    .apply(springSecurity())
                    .build();
            return new Service(application, mvc);
        }

        @SuppressWarnings("unchecked")
        void deliver(Fact fact) {
            var delivery = (Consumer<Map<String, Object>>) application.getBean("simulationEventDelivery");
            delivery.accept(fact.asDeliveredRecord(application.getBean(ObjectMapper.class)));
        }

        void configure(UUID caseKey) throws Exception {
            mvc.perform(put(EXECUTION)
                            .with(control())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(context(caseKey)))
                    .andExpect(status().isOk());
        }
    }
}
