package io.github.temporalrift.read.shared.infrastructure.adapter.in.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.shared.ProcessedEventPort;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;

class SimulationEventDeliveryTest {

    private static final List<String> HANDLED = new ArrayList<>();
    private static final String CLAIMED_EVENT = "7b0c1f2e-4a5d-4c3b-9e8f-0a1b2c3d4e5f";

    private AnnotationConfigApplicationContext context;
    private Consumer<Map<String, Object>> delivery;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        HANDLED.clear();
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("in-memory");
        context.register(Wiring.class, FirstGameListener.class, SecondGameListener.class, BothTopicsListener.class);
        context.refresh();
        delivery = (Consumer<Map<String, Object>>) context.getBean("simulationEventDelivery");
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void everyGroupSubscribedToTheTopicHandlesTheRecordOnce() {
        delivery.accept(record("game.events", "fine"));

        assertThat(HANDLED).containsExactlyInAnyOrder("first:fine", "second:fine", "both-game:fine");
    }

    @Test
    void aGroupOnAnotherTopicIsNotInvoked() {
        delivery.accept(record("timeline.events", "fine"));

        assertThat(HANDLED).containsExactly("both-timeline:fine");
    }

    @Test
    void recordsForOtherTopicsAreIgnored() {
        delivery.accept(record("other.events", "fine"));

        assertThat(HANDLED).isEmpty();
    }

    @Test
    void headersKeyAndPayloadReachTheHandlerAsTheyWouldFromKafka() {
        delivery.accept(record("timeline.events", "payload"));

        assertThat(HANDLED).containsExactly("both-timeline:payload");
        assertThat(BothTopicsListener.lastHeaders)
                .containsEntry("eventType", "Something")
                .containsEntry("kafka_receivedTopic", "timeline.events")
                .containsEntry("kafka_receivedMessageKey", "game-1");
    }

    @Test
    void aFailingGroupDoesNotStarveTheOthersAndTheCallerSeesTheFailure() {
        assertThatThrownBy(() -> delivery.accept(record("game.events", "fail")))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("first failed");

        assertThat(HANDLED).containsExactlyInAnyOrder("first:fail", "second:fail", "both-game:fail");
    }

    @Test
    void aFailingHandlerRollsItsClaimBackSoARedeliveryRunsItAgain() {
        assertThatThrownBy(() -> delivery.accept(record("game.events", "fail")))
                .isInstanceOf(IllegalStateException.class);
        HANDLED.clear();

        assertThatThrownBy(() -> delivery.accept(record("game.events", "fail")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(HANDLED).containsExactly("first:fail");
    }

    @Test
    void aRedeliveredRecordIsNotHandledASecondTimeByTheSameGroup() {
        delivery.accept(record("game.events", "fine"));
        HANDLED.clear();

        delivery.accept(record("game.events", "fine"));

        assertThat(HANDLED).isEmpty();
    }

    @Test
    void aMalformedRecordIsRejectedBeforeAnyHandlerRuns() {
        assertThatThrownBy(() -> delivery.accept(Map.of("topic", "game.events")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(HANDLED).isEmpty();
    }

    private static Map<String, Object> record(String topic, String body) {
        return Map.of(
                "topic",
                topic,
                "key",
                "game-1",
                "headers",
                Map.of(
                        "eventType",
                        "Something",
                        "eventId",
                        CLAIMED_EVENT,
                        "gameId",
                        UUID.randomUUID().toString()),
                "payload",
                body.getBytes(StandardCharsets.UTF_8));
    }

    private static String body(Message<Object> message) {
        return new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
    }

    private static void claimAndHandle(
            ProcessedEventPort processedEvents, String group, Message<Object> message, String fails) {
        var claimed = processedEvents.claim(UUID.fromString(CLAIMED_EVENT), group);
        if (!claimed) {
            return;
        }
        HANDLED.add(group + ":" + body(message));
        if (body(message).equals("fail") && group.equals(fails)) {
            throw new IllegalStateException(group + " failed");
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class Wiring {

        @Bean
        InMemoryDatabase database() {
            return new InMemoryDatabase();
        }

        @Bean
        PlatformTransactionManager transactionManager(InMemoryDatabase database) {
            return new InMemoryTransactionManager(database);
        }

        @Bean
        ProcessedEventPort processedEvents(InMemoryDatabase database) {
            return new TestClaims(database);
        }

        @Bean
        SimulationEventDelivery simulationEventDelivery(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
            return new SimulationEventDelivery(beanFactory);
        }
    }

    static class TestClaims implements ProcessedEventPort {

        private final io.github.temporalrift.read.shared.inmemory.Table<UUID, String> claims;

        TestClaims(InMemoryDatabase database) {
            this.claims = database.table("claims", false);
        }

        @Override
        @Transactional
        public boolean claim(UUID eventId, String consumer) {
            var key = io.github.temporalrift.read.shared.inmemory.RowKey.of(consumer);
            claims.lock(eventId, key);
            return claims.putIfAbsent(eventId, key, consumer);
        }
    }

    @org.springframework.stereotype.Component
    static class FirstGameListener {

        private final ProcessedEventPort processedEvents;

        FirstGameListener(ProcessedEventPort processedEvents) {
            this.processedEvents = processedEvents;
        }

        @KafkaListener(topics = "game.events", groupId = "first")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void handle(Message<Object> message) {
            claimAndHandle(processedEvents, "first", message, "first");
        }
    }

    @org.springframework.stereotype.Component
    static class SecondGameListener {

        private final ProcessedEventPort processedEvents;

        SecondGameListener(ProcessedEventPort processedEvents) {
            this.processedEvents = processedEvents;
        }

        @KafkaListener(topics = "game.events", groupId = "second")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        void handle(Message<Object> message) {
            claimAndHandle(processedEvents, "second", message, "first");
        }
    }

    @org.springframework.stereotype.Component
    static class BothTopicsListener {

        static Map<String, Object> lastHeaders = Map.of();

        private final ProcessedEventPort processedEvents;

        BothTopicsListener(ProcessedEventPort processedEvents) {
            this.processedEvents = processedEvents;
        }

        @KafkaListener(topics = "game.events", groupId = "both")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void handleGame(Message<Object> message) {
            claimAndHandle(processedEvents, "both-game", message, "none");
        }

        @KafkaListener(topics = "timeline.events", groupId = "both")
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void handleTimeline(Message<Object> message) {
            lastHeaders = Map.copyOf(message.getHeaders());
            claimAndHandle(processedEvents, "both-timeline", message, "none");
        }
    }
}
