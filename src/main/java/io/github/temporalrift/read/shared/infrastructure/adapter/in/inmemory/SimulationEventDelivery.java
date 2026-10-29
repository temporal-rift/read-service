package io.github.temporalrift.read.shared.infrastructure.adapter.in.inmemory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Inbound transport of the {@code in-memory} profile, in place of Kafka. A deliveredRecord is a map of JDK types only:
 * {@code topic}, {@code key} (strings), {@code headers} (a string-to-string map) and {@code payload} (the JSON
 * bytes). Delivery invokes, on the transactional bean, the unchanged listener method of every consumer group
 * subscribed to the topic and returns once their transactions have committed.
 *
 * <p>Retries and dead-lettering are not simulated: a failing group rolls back and its failure is reported to the
 * caller after the remaining groups have handled the deliveredRecord.
 *
 * <p>Every topic has the single partition 0; a group's position on it counts the records the group
 * handled, as a committed offset would.
 */
@Component("simulationEventDelivery")
@Profile("in-memory")
public class SimulationEventDelivery implements Consumer<Map<String, Object>>, SmartInitializingSingleton {

    private static final String APPLICATION_PACKAGE = "io.github.temporalrift.read";
    private static final int PARTITION = 0;

    private final ConfigurableListableBeanFactory beanFactory;
    private final Map<String, Map<String, Endpoint>> endpointsByTopicAndGroup = new HashMap<>();
    private final Map<GroupTopic, AtomicLong> handled = new ConcurrentHashMap<>();

    SimulationEventDelivery(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (var beanName : beanFactory.getBeanNamesForType(Object.class, false, false)) {
            var type = beanFactory.getType(beanName, false);
            if (type == null || !ClassUtils.getUserClass(type).getName().startsWith(APPLICATION_PACKAGE)) {
                continue;
            }
            var listenerMethods = MethodIntrospector.selectMethods(
                    ClassUtils.getUserClass(type), (MethodIntrospector.MetadataLookup<List<KafkaListener>>) method -> {
                        var listeners = new ArrayList<>(
                                AnnotatedElementUtils.getMergedRepeatableAnnotations(method, KafkaListener.class));
                        return listeners.isEmpty() ? null : listeners;
                    });
            if (!listenerMethods.isEmpty()) {
                register(beanFactory.getBean(beanName), listenerMethods);
            }
        }
    }

    private void register(Object bean, Map<Method, List<KafkaListener>> listenerMethods) {
        listenerMethods.forEach((method, listeners) -> {
            var invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
            ReflectionUtils.makeAccessible(invocable);
            for (var listener : listeners) {
                for (var topic : listener.topics()) {
                    endpointsByTopicAndGroup
                            .computeIfAbsent(topic, t -> new LinkedHashMap<>())
                            .putIfAbsent(listener.groupId(), new Endpoint(listener.groupId(), bean, invocable));
                }
            }
        });
    }

    @Override
    public void accept(Map<String, Object> deliveredRecord) {
        var topic = require(deliveredRecord, "topic", String.class);
        var message = message(topic, deliveredRecord);
        var failures = new ArrayList<Throwable>();
        for (var endpoint :
                endpointsByTopicAndGroup.getOrDefault(topic, Map.of()).values()) {
            try {
                endpoint.method().invoke(endpoint.bean(), message);
                handled.computeIfAbsent(new GroupTopic(endpoint.groupId(), topic), key -> new AtomicLong())
                        .incrementAndGet();
            } catch (InvocationTargetException e) {
                failures.add(e.getCause());
            } catch (IllegalAccessException | RuntimeException e) {
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            var failure = new IllegalStateException("Handling a " + topic + " record failed", failures.getFirst());
            failures.stream().skip(1).forEach(failure::addSuppressed);
            throw failure;
        }
    }

    /** Each consumer group's position on each topic it subscribes to: the offset of the next record it handles. */
    public List<ConsumerPosition> positions() {
        var positions = new ArrayList<ConsumerPosition>();
        endpointsByTopicAndGroup.forEach((topic, endpoints) -> endpoints
                .keySet()
                .forEach(groupId -> positions.add(new ConsumerPosition(
                        groupId,
                        topic,
                        PARTITION,
                        handled.getOrDefault(new GroupTopic(groupId, topic), new AtomicLong())
                                .get()))));
        positions.sort(Comparator.comparing(ConsumerPosition::groupId)
                .thenComparing(ConsumerPosition::topic)
                .thenComparingInt(ConsumerPosition::partition));
        return List.copyOf(positions);
    }

    private static Message<Object> message(String topic, Map<String, Object> deliveredRecord) {
        var headers = new HashMap<String, Object>();
        if (deliveredRecord.get("headers") instanceof Map<?, ?> recordHeaders) {
            recordHeaders.forEach((name, value) -> headers.put(String.valueOf(name), value));
        }
        headers.put(KafkaHeaders.RECEIVED_TOPIC, topic);
        if (deliveredRecord.get("key") != null) {
            headers.put(KafkaHeaders.RECEIVED_KEY, require(deliveredRecord, "key", String.class));
        }
        return MessageBuilder.<Object>withPayload(require(deliveredRecord, "payload", byte[].class))
                .copyHeaders(headers)
                .build();
    }

    private static <T> T require(Map<String, Object> deliveredRecord, String name, Class<T> type) {
        var value = deliveredRecord.get(name);
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Record entry '" + name + "' must be a " + type.getSimpleName() + " but was " + value);
        }
        return type.cast(value);
    }

    /** A consumer group's progress on one partition: the offset of the next record it will handle. */
    public record ConsumerPosition(String groupId, String topic, int partition, long nextOffset) {}

    private record Endpoint(String groupId, Object bean, Method method) {}

    private record GroupTopic(String groupId, String topic) {}
}
