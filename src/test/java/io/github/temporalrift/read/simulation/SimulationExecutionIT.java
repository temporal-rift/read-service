package io.github.temporalrift.read.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.SimulationIntegrationTest;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.simulation.SimulationFacts.Fact;
import io.github.temporalrift.read.simulation.SimulationFacts.Game;
import io.github.temporalrift.read.simulation.domain.port.out.SourceWatermarks;

/** The execution controls of the PostgreSQL profile, with records arriving through Kafka. */
@SimulationIntegrationTest
class SimulationExecutionIT extends SimulationExecutionContract {

    private static final JsonKafkaHeaderMapper HEADER_MAPPER = new JsonKafkaHeaderMapper();
    private static final Map<String, List<String>> GROUPS_BY_TOPIC = Map.of(
            SimulationFacts.GAME_EVENTS,
            List.of(
                    "read-service.projection.game-events",
                    "read-service.projection.game-history",
                    "read-service.notification-game-events"),
            SimulationFacts.TIMELINE_EVENTS,
            List.of(
                    "read-service.projection.timeline-events",
                    "read-service.projection.game-history",
                    "read-service.notification-timeline-events"));

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry listeners;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    protected MockMvc mvc() {
        return mvc;
    }

    @Override
    protected void deliver(Fact fact) {
        awaitHandled(fact.topic(), publish(fact));
    }

    @Override
    protected <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    protected <T> Collection<T> beans(Class<T> type) {
        return context.getBeansOfType(type).values();
    }

    @Override
    protected Set<String> gameplayTablesHoldingRows() {
        return jdbcTemplate.queryForList("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                          AND table_name NOT LIKE 'simulation\\_%' AND table_name NOT LIKE 'databasechangelog%'
                        """, String.class).stream()
                .filter(table -> Boolean.TRUE.equals(
                        jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table + ")", Boolean.class)))
                .collect(Collectors.toSet());
    }

    @Test
    void aRecordAlreadyInTheLogWhenItsContextClosedNeverReachesTheReplay() throws Exception {
        var caseKey = UUID.randomUUID();
        var game = new Game();
        configure(caseKey).andExpect(status().isOk());
        deliver(game.started);
        listeners.getListenerContainers().forEach(MessageListenerContainer::stop);
        var late = publish(game.ended);

        close(caseKey).andExpect(status().isNoContent());
        configure(caseKey).andExpect(status().isOk());
        listeners.getListenerContainers().forEach(MessageListenerContainer::start);
        awaitHandled(game.ended.topic(), late);

        checkpoint()
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.gameId").value(nullValue()));
        assertThat(bean(GameProjectionRepository.class).findByGameId(game.gameId))
                .isEmpty();

        deliver(game.started);

        checkpoint()
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.gameId").value(game.gameId.toString()));
        assertThat(bean(GameProjectionRepository.class)
                        .findByGameId(game.gameId)
                        .orElseThrow()
                        .phase())
                .isEqualTo(Phase.LOBBY);
    }

    @Test
    void aBacklogLeftWhileNoContextWasOpenNeitherActsNorBindsTheNextContext() throws Exception {
        var stray = new Game();
        listeners.getListenerContainers().forEach(MessageListenerContainer::stop);
        var backlog = publish(stray.started);

        configure(UUID.randomUUID()).andExpect(status().isOk());
        listeners.getListenerContainers().forEach(MessageListenerContainer::start);
        awaitHandled(stray.started.topic(), backlog);

        checkpoint()
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.gameId").value(nullValue()));
        assertThat(bean(GameProjectionRepository.class).findByGameId(stray.gameId))
                .isEmpty();

        var game = new Game();
        deliver(game.started);

        checkpoint()
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.gameId").value(game.gameId.toString()));
    }

    private RecordMetadata publish(Fact fact) {
        var producerRecord = new ProducerRecord<Object, Object>(
                fact.topic(), null, fact.gameId().toString(), fact.payload(objectMapper));
        HEADER_MAPPER.fromHeaders(
                new MessageHeaders(new HashMap<String, Object>(fact.headers())), producerRecord.headers());
        return kafkaTemplate.send(producerRecord).join().getRecordMetadata();
    }

    /** Waits until every group subscribed to the topic committed past the record, so it is handled or ignored. */
    private void awaitHandled(String topic, RecordMetadata published) {
        var watermarks = bean(SourceWatermarks.class);
        await().atMost(Duration.ofSeconds(60))
                .until(() -> GROUPS_BY_TOPIC.get(topic).stream()
                        .allMatch(group -> watermarks.current().stream()
                                .anyMatch(watermark -> watermark.groupId().equals(group)
                                        && watermark.topic().equals(topic)
                                        && watermark.partition() == published.partition()
                                        && watermark.nextOffset() > published.offset())));
    }
}
